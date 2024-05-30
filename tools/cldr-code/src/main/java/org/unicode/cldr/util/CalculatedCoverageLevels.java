package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.ToolConstants;

public class CalculatedCoverageLevels {
    /** Assumed level for root. CLDR-16420 */
    private static final Level DEFAULT_ROOT_LEVEL = Level.MODERN;

    /** locale to level */
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

    public Level getEffectiveCoverageLevel(String locale) {
        return getEffectiveCoverageLevel(CLDRLocale.getInstance(locale));
    }

    public Level getEffectiveCoverageLevel(CLDRLocale locale) {
        // per spec, assumed level for the explicit root locale
        if (locale == CLDRLocale.ROOT) {
            return DEFAULT_ROOT_LEVEL;
        }
        // See if there is an explicit entry
        final Level level = levels.get(locale.getBaseName());
        if (level != null) {
            return level;
        }
        // Otherwise, tail-recurse on parent, unless the parent is root
        final CLDRLocale parent = locale.getParent();
        if (parent == CLDRLocale.ROOT) {
            // not found: no level.
            // TODO: should this really be 'core'? if at least core? CLDR-16420
            return null;
        }
        return getEffectiveCoverageLevel(parent);
    }

    /**
     * Is the locale present in the list? If so, it is at least basic. Requires an exact match on
     * the locale
     *
     * @param locale
     * @return
     */
    public boolean isLocaleAtLeastBasic(String locale) {
        return levels.containsKey(locale);
    }

    /** Read the coverage levels from the standard file */
    static CalculatedCoverageLevels fromFile() throws IOException {
        return fromFile(CLDRPaths.COMMON_DIRECTORY);
    }

    /** Read the coverage levels from the specified dir */
    static CalculatedCoverageLevels fromFile(final String dir) throws IOException {
        try (BufferedReader r =
                FileUtilities.openUTF8Reader(dir + "properties/", "coverageLevels.txt"); ) {
            return fromReader(r);
        }
    }

    static CalculatedCoverageLevels forVersion(VersionInfo v) throws IOException {
        return fromFile(ToolConstants.getBaseDirectory(v) + "/common/");
    }

    /**
     * read the coverage levels from a BufferedReader
     *
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
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final List<String> l = SEMICOLON.splitToList(line);
            if (l.size() != 3) {
                throw new IllegalArgumentException(
                        "coverageLevels.txt:" + no + ": expected 2 fields, got " + l.size());
            }
            final String uloc = l.get(0);
            final String level = l.get(1);
            final String name = l.get(2);
            final Level lev = Level.fromString(level);
            if (levels.put(uloc, lev) != null) {
                throw new IllegalArgumentException(
                        "coverageLevels.txt:" + no + ": duplicate locale " + uloc);
            }
        }
        return new CalculatedCoverageLevels(levels);
    }

    private static final class CalculatedCoverageLevelsHelper {
        public CalculatedCoverageLevels levels;

        public CalculatedCoverageLevelsHelper() {
            try {
                levels = CalculatedCoverageLevels.fromFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.err.println("Could not load CalculatedCoverageLevels: " + ioe);
                levels = null;
            }
        }

        public static CalculatedCoverageLevelsHelper INSTANCE =
                new CalculatedCoverageLevelsHelper();
    }

    /**
     * Get the singleton.
     *
     * @return the singleton, or NULL if there was an error.
     */
    public static CalculatedCoverageLevels getInstance() {
        return CalculatedCoverageLevelsHelper.INSTANCE.levels;
    }
}
