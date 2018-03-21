package org.unicode.cldr.test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InputStreamFactory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.StringId;

import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * This class should be used to detect when a path should be included in the set
 * of outdated items, because the value in the locale has not changed since the
 * last time the English changed. For efficiency, it only keeps a record of
 * those values in trunk that are out of date.
 * <p>
 * That is, to get the set of outdated values, the caller should do the following:
 * <ol>
 * <li>Test to see if the user has voted for a value for the path. If so, don't include.
 * <li>Test to see if the winning value for the path is different from the trunk value. If so, don't include.
 * <li>Test with isOutdated(path) to see if the trunk value was outdated. If not, don't include.
 * <li>Otherwise, include this path in the set of outdated items.
 * </ol>
 * <p>
 * To update the data file, use GenerateBirth.java.
 */
public class OutdatedPaths {

    public static final String OUTDATED_DIR = "births/";
    public static final String OUTDATED_ENGLISH_DATA = "outdatedEnglish.data";
    public static final String OUTDATED_DATA = "outdated.data";

    private static final boolean DEBUG = CldrUtility.getProperty("OutdatedPathsDebug", false);

    private final HashMap<String, Set<Long>> localeToData = new HashMap<String, Set<Long>>();
    private final HashMap<Long, String> pathToPrevious = new HashMap<Long, String>();

    /**
     * Creates a new OutdatedPaths, using the data file "outdated.data" in the same directory as this class.
     *
     * @param version
     */
    public OutdatedPaths() {
        this(null);
    }

    /**
     * Loads the data from the specified directory, using the data file "outdated.data".
     *
     * @param directory
     */
    public OutdatedPaths(String directory) {
        try {
            DataInputStream dataIn = openDataInput(directory, OUTDATED_DATA);
            Map<Long, PathHeader> id2header = new HashMap<Long, PathHeader>();
            if (DEBUG) {
                Factory factory = CLDRConfig.getInstance().getMainAndAnnotationsFactory();
                id2header = getIdToPath(factory);
            }
            while (true) {
                String locale = dataIn.readUTF();
                if (locale.equals("$END$")) {
                    break;
                }
                if (DEBUG) {
                    System.out.println("OutdatedPaths: Locale: " + locale);
                }
                final HashSet<Long> data = new HashSet<Long>();
                int size = dataIn.readInt();
                for (int i = 0; i < size; ++i) {
                    long item = dataIn.readLong();
                    data.add(item);
                    if (DEBUG) {
                        System.out.println(locale + "\t" + id2header.get(item));
                    }
                }
                localeToData.put(locale, Collections.unmodifiableSet(data));
            }
            dataIn.close();

            // now previous English

            dataIn = openDataInput(directory, OUTDATED_ENGLISH_DATA);
            int size = dataIn.readInt();
            if (DEBUG) {
                System.out.println("English Data");
            }
            for (int i = 0; i < size; ++i) {
                long pathId = dataIn.readLong();
                String previous = dataIn.readUTF();
                if (DEBUG) {
                    System.out.println("en\t(" + previous + ")\t" + id2header.get(pathId));
                }
                pathToPrevious.put(pathId, previous);
            }
            String finalCheck = dataIn.readUTF();
            if (!finalCheck.equals("$END$")) {
                throw new IllegalArgumentException("Corrupted " + OUTDATED_ENGLISH_DATA);
            }
            dataIn.close();

        } catch (IOException e) {
            throw new ICUUncheckedIOException("Data Not Available", e);
        }
    }

    public Map<Long, PathHeader> getIdToPath(Factory factory) {
        Map<Long, PathHeader> result = new HashMap<Long, PathHeader>();
        CLDRFile english = factory.make("en", true);
        PathHeader.Factory pathHeaders = PathHeader.getFactory(english);
        for (String s : english) {
            long id = StringId.getId(s);
            PathHeader pathHeader = pathHeaders.fromPath(s);
            result.put(id, pathHeader);
        }
        return result;
    }

    @SuppressWarnings("resource")
    private DataInputStream openDataInput(String directory, String filename) throws FileNotFoundException {
        String dataFileName = filename;
        InputStream fileInputStream = directory == null
            ? CldrUtility.getInputStream(OUTDATED_DIR + dataFileName) :
            //: new FileInputStream(new File(directory, dataFileName));
            InputStreamFactory.createInputStream(new File(directory, dataFileName));
        DataInputStream dataIn = new DataInputStream(fileInputStream);
        return dataIn;
    }

    /**
     * Returns true if the value for the path is outdated in trunk. See class
     * description for more info.
     *
     * @param distinguishedPath
     * @return true if the string is outdated
     */
    public boolean isOutdated(String locale, String distinguishedPath) {
        Set<Long> data = localeToData.get(locale);
        if (data == null) {
            return false;
        }
        long id = StringId.getId(distinguishedPath);
        boolean result = data.contains(id);
        if (result == false) {
            return false;
        }
        Boolean toSkip = SKIP_PATHS.get(distinguishedPath);
        if (toSkip != null) {
            return false;
        }
        return result;
    }

    /**
     * The same as isOutdated, but also returns paths that aren't skipped.
     *
     * @param locale
     * @param distinguishedPath
     * @return
     */
    public boolean isRawOutdated(String locale, String distinguishedPath) {
        Set<Long> data = localeToData.get(locale);
        if (data == null) {
            return false;
        }
        long id = StringId.getId(distinguishedPath);
        return data.contains(id);
    }

    /**
     * Is this path to be skipped? (because the English is normally irrelevant).
     *
     * @param distinguishedPath
     * @return
     */
    public boolean isSkipped(String distinguishedPath) {
        return SKIP_PATHS.get(distinguishedPath) != null;
    }

    /**
     * Returns true if the value for the path is outdated in trunk. See class
     * description for more info.
     *
     * @param distinguishedPath
     * @return true if the string is outdated
     */
    public String getPreviousEnglish(String distinguishedPath) {
        long id = StringId.getId(distinguishedPath);
        return pathToPrevious.get(id);
    }

    static RegexLookup<Boolean> SKIP_PATHS = new RegexLookup<Boolean>()
        .add("/exemplarCharacters", true)
        .add("/references", true)
        .add("/delimiters/[^/]*uotation", true)
        .add("/posix", true)
        .add("/pattern", true)
        .add("/fields/field[^/]*/displayName", true)
        .add("/dateFormatItem", true)
        .add("/numbers/symbols", true)
        .add("/fallback", true)
        .add("/quarters", true)
        .add("/months", true);

    /**
     * Returns the number of outdated paths.
     *
     * @param locale
     * @return number of outdated paths.
     */
    public int countOutdated(String locale) {
        Set<Long> data = localeToData.get(locale);
        return data == null ? 0 : data.size();
    }

    public static OutdatedPaths getInstance() {
        OutdatedPaths outdatedPaths = SINGLETON.get();
        if (outdatedPaths == null) {
            outdatedPaths = new OutdatedPaths();
            SINGLETON = new SoftReference<OutdatedPaths>(outdatedPaths);
        }
        return outdatedPaths;
    }

    private static Reference<OutdatedPaths> SINGLETON = new SoftReference<OutdatedPaths>(null);
}
