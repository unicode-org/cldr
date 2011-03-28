package org.unicode.cldr.test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.StringId;

/**
 * This class should be used to detect when a path should be included in the set
 * of outdated items, because the value in the locale has not changed since the
 * last time the English changed. For efficiency, it only keeps a record of
 * those values in trunk that are out of date.
 * <p>
 * That is, to get the set of outdated values, the caller should do the
 * following:
 * <ol>
 * <li>Test to see if the user has voted for a value for the path. If so, don't
 * include.
 * <li>Test to see if the winning value for the path is different from the trunk
 * value. If so, don't include.
 * <li>Test with isOutdated(path) to see if the trunk value was outdated. If
 * not, don't include.
 * <li>Otherwise, include this path in the set of outdated items.
 * </ol>
 * <p>
 * To update the data file, use GenerateBirth.java.
 */
public class OutdatedPaths {
    private static final boolean       DEBUG = false;
    private final HashMap<String, Set<Long>> localeToData = new HashMap<String, Set<Long>>();

    /**
     * Creates a new OutdatedPaths, using the data file "outdated.data" in the same directory as this class.
     * 
     * @param directory
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
            String dataFileName = "outdated.data";
            InputStream fileInputStream = directory == null 
            ? OutdatedPaths.class.getResourceAsStream(dataFileName) 
                    : new FileInputStream(new File(directory, dataFileName));

            DataInputStream dataIn = new DataInputStream(fileInputStream);
            while (true) {
                String locale = dataIn.readUTF();
                if (locale.equals("$END$")) {
                    break;
                }
                final HashSet<Long> data = new HashSet<Long>();
                int size = dataIn.readInt();
                for (int i = 0; i < size; ++i) {
                    long item = dataIn.readLong();
                    data.add(item);
                    if (DEBUG) {
                        System.out.println(item);
                    }
                }
                localeToData.put(locale, data);
            }
            dataIn.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Data Not Available", e);
        }
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
        if (toSkip != null)  {
            return false;
        }
        return result;
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
    .add("/months", true)
    ;
    
    /**
     * Returns the number of outdated paths.
     * @param locale
     * @return number of outdated paths.
     */
    public int countOutdated(String locale) {
        Set<Long> data = localeToData.get(locale);
        return data == null ? 0 : data.size();
    }
}
