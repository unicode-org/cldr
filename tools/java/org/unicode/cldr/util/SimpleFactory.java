package org.unicode.cldr.util;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.DraftStatus;

public class SimpleFactory extends Factory {
    /**
     * The maximum cache size the caches in
     * 15 is a safe limit for instances with limited amounts of memory (around 128MB).
     * Larger numbers are tolerable if more memory is available.
     * This constant may be moved to CldrUtilities in future if needed.
     */
    private static final int CACHE_LIMIT = 15;

    private volatile CLDRFile result; // used in handleMake
    private File sourceDirectories[];
    private String matchString;
    private Set<String> localeList = new TreeSet<String>();
    private Map<String, CLDRFile>[] mainCache = new Map[DraftStatus.values().length];
    private Map<String, CLDRFile>[] resolvedCache = new Map[DraftStatus.values().length];
    {
        for (int i = 0; i < mainCache.length; ++i) {
            mainCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
            resolvedCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
        }
    }

    private DraftStatus minimalDraftStatus = DraftStatus.unconfirmed;

    private SimpleFactory() {
    }

    public DraftStatus getMinimalDraftStatus() {
        return minimalDraftStatus;
    }

    /**
     * Create a factory from a source directory, matchingString
     * For the matchString meaning, see {@link getMatchingXMLFiles}
     */
    public static Factory make(String sourceDirectory, String matchString) {
        return make(sourceDirectory, matchString, DraftStatus.unconfirmed);
    }

    public static Factory make(String sourceDirectory, String matchString, DraftStatus minimalDraftStatus) {
        File list[] = { new File(sourceDirectory) };
        return new SimpleFactory(list, matchString, minimalDraftStatus);
    }

    /**
     * Create a factory from a source directory list, matchingString.
     * For the matchString meaning, see {@link getMatchingXMLFiles}
     */
    public static Factory make(File sourceDirectory[], String matchString) {
        return make(sourceDirectory, matchString, DraftStatus.unconfirmed);
    }

    /**
     * Create a factory from a source directory list
     * 
     * @param sourceDirectory
     * @param matchString
     * @param minimalDraftStatus
     * @return
     */
    public static Factory make(File sourceDirectory[], String matchString, DraftStatus minimalDraftStatus) {
        return new SimpleFactory(sourceDirectory, matchString, minimalDraftStatus);
    }

    private SimpleFactory(File sourceDirectories[], String matchString, DraftStatus minimalDraftStatus) {
        this.sourceDirectories = sourceDirectories;
        this.matchString = matchString;
        this.minimalDraftStatus = minimalDraftStatus;
        Matcher m = Pattern.compile(matchString).matcher("");
        this.localeList = CLDRFile.getMatchingXMLFiles(sourceDirectories, m);
        File goodSuppDir = null;
        for (File sourceDirectoryPossibility : sourceDirectories) {
            File suppDir = new File(sourceDirectoryPossibility, "../supplemental");
            if (suppDir.isDirectory()) {
                goodSuppDir = suppDir;
                break;
            }
        }
        if (goodSuppDir != null) {
            setSupplementalDirectory(goodSuppDir);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{" + getClass().getName())
            .append(" dirs=");
        for (File f : sourceDirectories) {
            sb.append(f.getPath()).append(' ');
        }
        sb.append('}');
        return sb.toString();
    }

    protected Set<String> handleGetAvailable() {
        return localeList;
    }

    /**
     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
     * use clone().
     */
    public CLDRFile handleMake(String localeName, boolean resolved, DraftStatus minimalDraftStatus) {
        Map<String, CLDRFile> cache = resolved ? resolvedCache[minimalDraftStatus.ordinal()]
            : mainCache[minimalDraftStatus.ordinal()];
        // Use double-check idiom.
        result = cache.get(localeName);
        if (result != null) return result;
        synchronized (cache) {
            // Check cache twice to ensure that CLDRFile is only loaded once
            // even with multiple threads.
            result = cache.get(localeName);
            if (result != null) return result;
            if (resolved) {
                result = new CLDRFile(makeResolvingSource(localeName, minimalDraftStatus));
            } else {
                final File parentDir = getSourceDirectoryForLocale(localeName);
                if (parentDir != null) {
                    result = makeFile(localeName, parentDir, minimalDraftStatus);
                    result.freeze();
                }
            }
            if (result != null) {
                cache.put(localeName, result);
            }
            return result;
        }
    }

    /**
     * Produce a CLDRFile from a localeName, given a directory.
     * 
     * @param localeName
     * @param dir
     *            directory
     */
    // TODO make the directory a URL
    public static CLDRFile makeFromFile(String fullFileName, String localeName, DraftStatus minimalDraftStatus) {
        return makeFromFile(new File(fullFileName), localeName, minimalDraftStatus);
    }

    private static CLDRFile makeFromFile(File file, String localeName, DraftStatus minimalDraftStatus) {
        return CLDRFile.loadFromFile(file, localeName, minimalDraftStatus);
    }

    /**
     * Create a CLDRFile for the given localename.
     * 
     * @param localeName
     */
    public static CLDRFile makeSupplemental(String localeName) {
        XMLSource source = new SimpleXMLSource(localeName);
        CLDRFile result = new CLDRFile(source);
        result.setNonInheriting(true);
        return result;
    }

    /**
     * CLDRFile from a file input stream. Set the locale ID from the same input stream.
     * 
     * @param fileName
     * @param fis
     * @param minimalDraftStatus
     * @return
     */
    public static CLDRFile makeFile(String fileName, InputStream fis, CLDRFile.DraftStatus minimalDraftStatus) {
        CLDRFile file = CLDRFile.load(fileName, null, fis, minimalDraftStatus);
        return file;
    }

    /**
     * Produce a CLDRFile from a file input stream.
     * 
     * @param localeName
     * @param fis
     */
    public static CLDRFile makeFile(String fileName, String localeName, InputStream fis,
        CLDRFile.DraftStatus minimalDraftStatus) {
        return CLDRFile.load(fileName, localeName, fis, minimalDraftStatus);
    }

    public static CLDRFile makeFile(String localeName, String dir, CLDRFile.DraftStatus minimalDraftStatus) {
        return makeFile(localeName, new File(dir), minimalDraftStatus);
    }

    public static CLDRFile makeFile(String localeName, File dir, CLDRFile.DraftStatus minimalDraftStatus) {
        CLDRFile file = makeFromFile(makeFileName(localeName, dir), localeName, minimalDraftStatus);
        return file;
    }

    /**
     * @param localeName
     * @param dir
     * @return
     */
    private static File makeFileName(String localeName, File dir) {
        return new File(dir, localeName + ".xml");
    }

    /**
     * Create a CLDRFile for the given localename.
     * SimpleXMLSource will be used as the source.
     * 
     * @param localeName
     */
    public static CLDRFile makeFile(String localeName) {
        XMLSource source = new SimpleXMLSource(localeName);
        return new CLDRFile(source);
    }

    /**
     * Produce a CLDRFile from a localeName and filename, given a directory.
     * 
     * @param localeName
     * @param dir
     *            directory
     */
    public static CLDRFile makeFile(String localeName, String dir, boolean includeDraft) {
        return makeFile(localeName, dir, includeDraft ? CLDRFile.DraftStatus.unconfirmed
            : CLDRFile.DraftStatus.approved);
    }

    @Override
    public File[] getSourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public File getSourceDirectoryForLocale(String localeName) {
        boolean isSupplemental = CLDRFile.isSupplementalName(localeName);
        for (File sourceDirectory : this.sourceDirectories) {
            if (isSupplemental) {
                sourceDirectory = new File(sourceDirectory.getAbsolutePath().
                    replace("incoming" + File.separator + "vetted" + File.separator, "common" + File.separator));
            }
            final File dir = isSupplemental ? new File(sourceDirectory, "../supplemental") : sourceDirectory;
            final File xmlFile = makeFileName(localeName, dir);
            if (xmlFile.canRead()) {
                return dir;
            }
        }
        return null;
    }

}