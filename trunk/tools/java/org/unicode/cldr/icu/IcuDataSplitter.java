package org.unicode.cldr.icu;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.icu.ResourceSplitter.SplitInfo;

public class IcuDataSplitter {
    private static final String VERSION_PATH = "/Version";
    private static final String PARENT_PATH = "/%%Parent";

    private final List<SplitInfo> splitInfos;
    private final Map<String, File> targetDirs;
    private final Map<String, Set<String>> splitSources = new HashMap<String, Set<String>>();

    /**
     * Splits the
     *
     * @param splitInfos
     */
    private IcuDataSplitter(List<SplitInfo> splitInfos) {
        this.splitInfos = splitInfos;
        targetDirs = new HashMap<String, File>();
    }

    /**
     * Creates a new IcuDataSplitter and creates directories for the split files
     * if they do not already exist.
     *
     * @param mainDirPath
     *            the main directory that other directories will be relative to.
     * @param splitInfos
     * @return
     */
    public static IcuDataSplitter make(String mainDirPath, List<SplitInfo> splitInfos) {
        IcuDataSplitter splitter = new IcuDataSplitter(splitInfos);
        // Make sure that all the required directories are present.
        Map<String, File> targetDirs = splitter.targetDirs;
        for (SplitInfo si : splitInfos) {
            String dirPath = si.targetDirPath;
            if (!targetDirs.containsKey(dirPath)) {
                File dir = new File(dirPath);
                if (!dir.isAbsolute()) {
                    dir = new File(mainDirPath, "/../" + dirPath);
                }
                if (dir.exists()) {
                    if (!dir.isDirectory()) {
                        throw new IllegalArgumentException(
                            "File \"" + dirPath + "\" exists and is not a directory");
                    }
                    if (!dir.canWrite()) {
                        throw new IllegalArgumentException(
                            "Cannot write to directory \"" + dirPath + "\"");
                    }
                } else {
                    if (!dir.mkdirs()) {
                        String canonicalPath;
                        try {
                            canonicalPath = dir.getCanonicalPath();
                        } catch (IOException e) {
                            canonicalPath = dirPath;
                        }
                        throw new IllegalArgumentException(
                            "Unable to create directory path \"" + canonicalPath + "\"");
                    }
                }
                targetDirs.put(dirPath, dir);
            }
        }
        return splitter;
    }

    /**
     * Splits an IcuData object for writing to different directories.
     *
     * @param data
     * @return
     */
    public Map<String, IcuData> split(IcuData icuData, String fallbackDir) {
        Map<String, IcuData> splitData = new HashMap<String, IcuData>();
        String sourceFile = icuData.getSourceFile();
        String name = icuData.getName();
        boolean hasFallback = icuData.hasFallback();
        Set<String> dirs = targetDirs.keySet();
        for (String dir : dirs) {
            splitData.put(dir, new IcuData(sourceFile, name, hasFallback));
        }
        splitData.put(fallbackDir, new IcuData(sourceFile, name, hasFallback));

        for (Entry<String, List<String[]>> entry : icuData.entrySet()) {
            String rbPath = entry.getKey();
            List<String[]> values = entry.getValue();
            boolean wasSplit = false;
            // Paths that should be copied to all directories.
            if (rbPath.equals(VERSION_PATH) || rbPath.equals(PARENT_PATH)) {
                for (String dir : dirs) {
                    splitData.get(dir).addAll(rbPath, values);
                }
            } else {
                // Split up regular paths.
                for (SplitInfo splitInfo : splitInfos) {
                    String checkPath = rbPath.replaceFirst(":alias", "/"); // Handle splitting of a top level alias ( as in root/units )
                    if (checkPath.startsWith(splitInfo.srcNodePath)) {
                        splitData.get(splitInfo.targetDirPath).addAll(rbPath, values);
                        wasSplit = true;
                        break;
                    }
                }
            }
            // Add any remaining values to the file in fallback dir.
            if (!wasSplit) {
                splitData.get(fallbackDir).addAll(rbPath, values);
            }
        }
        // Remove all files that only contain version info.
        Iterator<Entry<String, IcuData>> iterator = splitData.entrySet().iterator();
        String comment = icuData.getFileComment();
        while (iterator.hasNext()) {
            Entry<String, IcuData> entry = iterator.next();
            IcuData data = entry.getValue();
            data.setFileComment(comment);
            if (entry.getKey().equals(fallbackDir)) continue;
            if (data.size() == 1 && data.containsKey(VERSION_PATH)) {
                // Comment copied from ResourceSplitter:
                // Some locales that use root data rely on the presence of
                // a resource file matching the prefix of the locale to prevent fallback
                // lookup through the default locale. To prevent this error, all resources
                // need at least a language-only stub resource to be present.
                //
                // Arrgh. The icu package tool wants all internal nodes in the tree to be
                // present. Currently, the missing nodes are all lang_Script locales.
                // Maybe change the package tool to fix this.
                String locale = data.getName();
                int underscorePos = locale.indexOf('_');
                if (underscorePos > -1 && locale.length() - underscorePos - 1 != 4) {
                    iterator.remove();
                    continue;
                }
            }
            add(splitSources, entry.getKey(), data.getName());
        }
        return splitData;
    }

    /**
     * Adds a value to the list with the specified key.
     */
    private static void add(Map<String, Set<String>> map, String key, String value) {
        Set<String> set = map.get(key);
        if (set == null) {
            map.put(key, set = new HashSet<String>());
        }
        set.add(value);
    }

    /**
     * Returns the set of directories that the splitter splits data into (excluding the main directory).
     */
    public Set<String> getTargetDirs() {
        return targetDirs.keySet();
    }

    public Set<String> getDirSources(String dir) {
        return Collections.unmodifiableSet(splitSources.get(dir));
    }

    public Makefile generateMakefile(Collection<String> aliases, String dir) {
        String prefix = dir.toUpperCase();
        Makefile makefile = new Makefile(prefix);
        makefile.addSyntheticAlias(aliases);
        makefile.addAliasSource();
        makefile.addSource(splitSources.get(dir));
        return makefile;
    }
}
