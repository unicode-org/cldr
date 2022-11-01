package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.Splitter;


public class CalculatedCoverageLevels {
    final Map<String, Level> levels;

    protected CalculatedCoverageLevels(Map<String, Level> levels) {
        this.levels = Collections.unmodifiableMap(levels);
    }

    /**
     * @return unmodifiable map
     */
    public Map<String, Level> getLevels() {
        return levels;
    }

    /**
     * Is the locale present in the list? If so, it is at least basic.
     * Requires an exact match on the locale
     * @param locale
     * @return
     */
    public boolean isLocaleAtLeastBasic(String locale) {
        return levels.containsKey(locale);
    }

    /**
     * Read the coverage levels from the standard file
     */
    static CalculatedCoverageLevels fromFile() throws IOException {
        try (BufferedReader r = FileUtilities.openUTF8Reader(CLDRPaths.COMMON_DIRECTORY + "properties/", "coverageLevels.txt");) {
            return fromReader(r);
        }
    }

    /**
     * read the coverage levels from a BufferedReader
     * @param r
     * @return
     * @throws IOException
     */
    static CalculatedCoverageLevels fromReader(BufferedReader r) throws IOException {
        Map<String, Level> levels = new TreeMap<>();
        final Splitter SEMICOLON = Splitter.on(';').trimResults();
        String line;
        int no = 0;
        while ((line = r.readLine()) != null) {
            no++;
            line = line.trim();
            if(line.startsWith("#") || line.isBlank()) {
                continue;
            }
            final List<String> l = SEMICOLON.splitToList(line);
            if (l.size() != 2) {
                throw new IllegalArgumentException("coverageLevels.txt:"+no+": expected 2 fields, got " + l.size());
            }
            final String uloc = l.get(0);
            final String level = l.get(1);
            final Level lev = Level.fromString(level);
            if (levels.put(uloc, lev) != null) {
                throw new IllegalArgumentException("coverageLevels.txt:"+no+": duplicate locale " + uloc);
            }
        }
        return new CalculatedCoverageLevels(levels);
    }

    private static final class CalculatedCoverageLevelsHelper {
        public CalculatedCoverageLevels levels;
        public CalculatedCoverageLevelsHelper() {
            try {
                levels = CalculatedCoverageLevels.fromFile();
            } catch(IOException ioe) {
                ioe.printStackTrace();
                System.err.println("Could not load CalculatedCoverageLevels: " + ioe);
                levels = null;
            }
        }
        public static CalculatedCoverageLevelsHelper INSTANCE = new CalculatedCoverageLevelsHelper();
    }

    /**
     * Get the singleton.
     * @return the singleton, or NULL if there was an error.
     */
    public static CalculatedCoverageLevels getInstance() {
        return CalculatedCoverageLevelsHelper.INSTANCE.levels;
    }
}
