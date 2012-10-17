package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.util.ULocale;

/**
 * Factory for filtering CLDRFiles by organization.
 * Organization coverage data is in org/unicode/cldr/util/data/Locales.txt.
 * 
 * @author jchye
 * 
 */
public class FilterFactory extends Factory {
    private static final String[] SPECIAL_ALT_PATHS = {
        "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]",
        "//ldml/localeDisplayNames/territories/territory[@type=\"MO\"][@alt=\"short\"]",
        "//ldml/localeDisplayNames/territories/territory[@type=\"MK\"][@alt=\"variant\"]"
    };

    private Factory rawFactory;
    private String organization;
    private SupplementalDataInfo supplementalData;
    private boolean useAltValues;

    /**
     * Creates a new Factory for filtering CLDRFiles.
     * 
     * @param rawFactory
     *            the factory to be filtered
     * @param organization
     *            the organization that the filtering is catered towards
     * @param useAltValues
     *            true if certain alt values should be used to replace the main values
     */
    public FilterFactory(Factory rawFactory, String organization, boolean useAltValues) {
        this.rawFactory = rawFactory;
        this.organization = organization;
        supplementalData = SupplementalDataInfo.getInstance();
        setSupplementalDirectory(rawFactory.getSupplementalDirectory());
        this.useAltValues = useAltValues;
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
            return filterCldrFile(localeID, minimalDraftStatus);
        }
    }

    /**
     * @return a filtered CLDRFile.
     */
    private CLDRFile filterCldrFile(String localeID, DraftStatus minimalDraftStatus) {
        CLDRFile rawFile = rawFactory.make(localeID, false, minimalDraftStatus).cloneAsThawed();

        filterAltValues(rawFile);
        filterCoverage(rawFile);
        return rawFile;
    }

    /**
     * Replaces the value for certain XPaths with their alternate value.
     * 
     * @param rawFile
     */
    private void filterAltValues(CLDRFile rawFile) {
        if (!useAltValues) return;

        // For certain alternate values, use them as the main values.
        for (String altPath : SPECIAL_ALT_PATHS) {
            String altValue = rawFile.getStringValue(altPath);
            if (altValue != null) {
                String mainPath = altPath.substring(0, altPath.lastIndexOf('['));
                rawFile.add(mainPath, altValue);
                rawFile.remove(altPath);
            }
        }
    }

    /**
     * Filters a CLDRFile according to the specified organization's coverage level.
     * 
     * @param rawFile
     */
    private void filterCoverage(CLDRFile rawFile) {
        if (organization == null) return;

        int minLevel = StandardCodes.make()
            .getLocaleCoverageLevel(organization, rawFile.getLocaleID())
            .getLevel();
        ULocale locale = new ULocale(rawFile.getLocaleID());
        for (String xpath : rawFile) {
            int level = supplementalData.getCoverageValue(xpath, locale);
            if (level > minLevel) {
                rawFile.remove(xpath);
            }
        }
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
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Factory rawFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        // TODO: generalize this for other organizations.
        Factory filterFactory = new FilterFactory(rawFactory, "google", true);
        String outputDir = CldrUtility.GEN_DIRECTORY + "/filter";
        for (String locale : rawFactory.getAvailable()) {
            PrintWriter out = BagFormatter.openUTF8Writer(outputDir, locale + ".xml");
            filterFactory.make(locale, false).write(out);
            out.close();
        }
    }
}
