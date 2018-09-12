package org.unicode.cldr.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRFile.DraftStatus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class SimpleFactory extends Factory {

    /**
     * Variable to control the behaviour of the class.
     *  TRUE -  use a (non-static) array of Maps, indexed by locale String (old behaviour)
     *  FALSE - use a single static map, indexed with a more elaborate key.
     */
    private static final boolean USE_OLD_HANDLEMAKE_CODE = false;

    /**
     * Variable that customizes the caching of the results of SimpleFactory.make
     *
     */
    private static final boolean CACHE_SIMPLE_FACTORIES = false;

    /**
     * Number of Factories that should be cached, if caching of factories is enabled
     */
    private static final int FACTORY_CACHE_LIMIT = 10;

    /**
     * Object that is used for synchronization when looking up simple factories
     */
    private static final Object FACTORY_LOOKUP_SYNC = new Object();
    /**
     * The maximum cache size the caches in
     * 15 is a safe limit for instances with limited amounts of memory (around 128MB).
     * Larger numbers are tolerable if more memory is available.
     * This constant may be moved to CldrUtilities in future if needed.
     */
//    private static final int CACHE_LIMIT = 15;

    private static final int CACHE_LIMIT = 75;

    private static final boolean DEBUG_SIMPLEFACTORY = false;

    /**
     * Simple class used as a key for the map that holds the CLDRFiles -only used in the new version of the code
     * @author ribnitz
     *
     */
    private static class CLDRCacheKey {
        private final String localeName;
        private final boolean resolved;
        private final DraftStatus draftStatus;
        private final Set<String> directories; // ordered
        private final int hashCode;

        public CLDRCacheKey(String localeName, boolean resolved, DraftStatus draftStatus, List<File> directories) {
            super();
            this.localeName = localeName;
            this.resolved = resolved;
            this.draftStatus = draftStatus;
            // Parameter check: the directory/file supplied must be non-null and readable.
            if (directories == null || directories.isEmpty()) {
                throw new ICUUncheckedIOException("Attempt to create a CLDRCacheKey with a null directory, please supply a non-null one.");
            }
            ImmutableSet.Builder<String> _directories = ImmutableSet.builder();
            for (File directory : directories) {
                if (!directory.canRead()) {
                    throw new ICUUncheckedIOException("The directory specified, " + directory.getPath() + ", cannot be read");
                }
                _directories.add(directory.toString());
            }
            this.directories = _directories.build();
            hashCode = Objects.hash(this.localeName, this.resolved, this.draftStatus, this.directories);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CLDRCacheKey other = (CLDRCacheKey) obj;
            if (!Objects.equals(directories, other.directories)) {
                return false;
            }
            if (draftStatus != other.draftStatus) {
                return false;
            }
            if (localeName == null) {
                if (other.localeName != null) {
                    return false;
                }
            } else if (!localeName.equals(other.localeName)) {
                return false;
            }
            if (resolved != other.resolved) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "[ LocaleName: " + localeName + " Resolved: " + resolved + " Draft status: " + draftStatus + " Directories: " + directories + " ]";
        }
    }

    /**
     * If a SimpleDFactory covers more than one directory, SimpleFactoryLookupKey Objects may
     * be needed to find the SimpleFactory that is responsible for the given directory
     * @author ribnitz
     *
     */
    private static class SimpleFactoryLookupKey {
        private final String directory;
        private final String matchString;

        public SimpleFactoryLookupKey(String directory, String matchString) {
            this.directory = directory;
            this.matchString = matchString;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((directory == null) ? 0 : directory.hashCode());
            result = prime * result + ((matchString == null) ? 0 : matchString.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SimpleFactoryLookupKey other = (SimpleFactoryLookupKey) obj;
            if (directory == null) {
                if (other.directory != null) {
                    return false;
                }
            } else if (!directory.equals(other.directory)) {
                return false;
            }
            if (matchString == null) {
                if (other.matchString != null) {
                    return false;
                }
            } else if (!matchString.equals(other.matchString)) {
                return false;
            }
            return true;
        }

        public String getDirectory() {
            return directory;
        }

        public String getMatchString() {
            return matchString;
        }

        @Override
        public String toString() {
            return "SimpleFactoryLookupKey [directory=" + directory + ", matchString=" + matchString + "]";
        }

    }

    /**
     * Simple class to use as a Key in a map that caches SimpleFacotry instances.
     * @author ribnitz
     *
     */
    private static class SimpleFactoryCacheKey {
        private List<String> sourceDirectories;
        private String matchString;
        private DraftStatus mimimalDraftStatus;

        public SimpleFactoryCacheKey(List<String> sourceDirectories, String matchString, DraftStatus mimimalDraftStatus) {
            this.sourceDirectories = sourceDirectories;
            this.matchString = matchString;
            this.mimimalDraftStatus = mimimalDraftStatus;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((matchString == null) ? 0 : matchString.hashCode());
            result = prime * result + ((mimimalDraftStatus == null) ? 0 : mimimalDraftStatus.hashCode());
            result = prime * result + ((sourceDirectories == null) ? 0 : sourceDirectories.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SimpleFactoryCacheKey other = (SimpleFactoryCacheKey) obj;
            if (matchString == null) {
                if (other.matchString != null) {
                    return false;
                }
            } else if (!matchString.equals(other.matchString)) {
                return false;
            }
            if (mimimalDraftStatus != other.mimimalDraftStatus) {
                return false;
            }
            if (sourceDirectories == null) {
                if (other.sourceDirectories != null) {
                    return false;
                }
            } else if (!sourceDirectories.equals(other.sourceDirectories)) {
                return false;
            }
            return true;
        }

        public List<String> getSourceDirectories() {
            return sourceDirectories;
        }

        public String getMatchString() {
            return matchString;
        }

        public DraftStatus getMimimalDraftStatus() {
            return mimimalDraftStatus;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("SimpleFactoryCacheKey [sourceDirectories=").append(sourceDirectories).append(", matchString=").append(matchString)
                .append(", mimimalDraftStatus=").append(mimimalDraftStatus).append("]");
            return builder.toString();
        }

    }

    // private volatile CLDRFile result; // used in handleMake
    private File sourceDirectories[];
    private Set<String> localeList = new TreeSet<String>();
    private Cache<CLDRCacheKey, CLDRFile> combinedCache = null;
    //private   Map<CLDRCacheKey,CLDRFile> combinedCache=  null;
    //     Collections.synchronizedMap(new LruMap<CLDRCacheKey, CLDRFile>(CACHE_LIMIT));

    private Map<String, CLDRFile>[] mainCache = null; /* new Map[DraftStatus.values().length]; */
    private Map<String, CLDRFile>[] resolvedCache = null; /*new Map[DraftStatus.values().length]; */
//    {
//        for (int i = 0; i < mainCache.length; ++i) {
//            mainCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
//            resolvedCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
//        }
//    }
    private DraftStatus minimalDraftStatus = DraftStatus.unconfirmed;

    /* Use WeakValues - automagically remove a value once it is no longer useed elsewhere */
    private static Cache<SimpleFactoryCacheKey, SimpleFactory> factoryCache = null;
    // private static LockSupportMap<SimpleFactoryCacheKey> factoryCacheLocks=new LockSupportMap<>();
    private static Cache<SimpleFactoryLookupKey, SimpleFactoryCacheKey> factoryLookupMap = null;

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
        if (!CACHE_SIMPLE_FACTORIES) {
            return new SimpleFactory(list, matchString, minimalDraftStatus);
        }
        // we cache simple factories
        final String sourceDirPathName = list[0].getAbsolutePath();
        List<String> strList = Arrays.asList(new String[] { sourceDirPathName });
        final SimpleFactoryCacheKey key = new SimpleFactoryCacheKey(strList, matchString, minimalDraftStatus);

        synchronized (FACTORY_LOOKUP_SYNC) {
            if (factoryCache == null) {
                factoryCache = CacheBuilder.newBuilder().maximumSize(FACTORY_CACHE_LIMIT).build();
            }
            SimpleFactory fact = factoryCache.getIfPresent(key);
            if (fact == null) {
                // try looking it up
                SimpleFactoryLookupKey lookupKey = new SimpleFactoryLookupKey(sourceDirPathName, matchString);
                if (factoryLookupMap == null) {
                    factoryLookupMap = CacheBuilder.newBuilder().maximumSize(FACTORY_CACHE_LIMIT).build();
                }
                SimpleFactoryCacheKey key2 = factoryLookupMap.getIfPresent(lookupKey);
                if (key2 != null) {
                    return factoryCache.asMap().get(key2);
                }
                // out of luck
                SimpleFactory sf = new SimpleFactory(list, matchString, minimalDraftStatus);
                factoryCache.put(key, sf);
                if (DEBUG_SIMPLEFACTORY) {
                    System.out.println("Created new Factory with parameters " + key);
                }
                factoryLookupMap.put(lookupKey, key);
            }
            return factoryCache.asMap().get(key);
        }
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
        if (!CACHE_SIMPLE_FACTORIES) {
            return new SimpleFactory(sourceDirectory, matchString, minimalDraftStatus);
        }

        // we cache simple factories
        List<String> strList = new ArrayList<>();
        List<SimpleFactoryLookupKey> lookupList = new ArrayList<>();
        for (int i = 0; i < sourceDirectory.length; i++) {
            String cur = sourceDirectory[i].getAbsolutePath();
            strList.add(cur);
            lookupList.add(new SimpleFactoryLookupKey(cur, matchString));
        }
        final SimpleFactoryCacheKey key = new SimpleFactoryCacheKey(strList, matchString, minimalDraftStatus);
        synchronized (FACTORY_LOOKUP_SYNC) {
            if (factoryCache == null) {
                factoryCache = CacheBuilder.newBuilder().maximumSize(FACTORY_CACHE_LIMIT).build();
            }
            SimpleFactory fact = factoryCache.getIfPresent(key);
            if (fact == null) {
                if (factoryLookupMap == null) {
                    factoryLookupMap = CacheBuilder.newBuilder().maximumSize(FACTORY_CACHE_LIMIT).build();
                }
                Iterator<SimpleFactoryLookupKey> iter = lookupList.iterator();
                while (iter.hasNext()) {
                    SimpleFactoryLookupKey curKey = iter.next();
                    SimpleFactoryCacheKey key2 = factoryLookupMap.asMap().get(curKey);
                    if ((key2 != null) && factoryCache.asMap().containsKey(key2)) {
                        if (DEBUG_SIMPLEFACTORY) {
                            System.out.println("Using key " + key2 + " instead of " + key + " for factory lookup");
                        }
                        return factoryCache.asMap().get(key2);
                    }
                }
                SimpleFactory sf = new SimpleFactory(sourceDirectory, matchString, minimalDraftStatus);
                if (DEBUG_SIMPLEFACTORY) {
                    System.out.println("Created new Factory with parameters " + key);
                }
                factoryCache.put(key, sf);
                iter = lookupList.iterator();
                while (iter.hasNext()) {
                    factoryLookupMap.put(iter.next(), key);
                }
            }
            return factoryCache.asMap().get(key);
        }
    }

    @SuppressWarnings("unchecked")
    private SimpleFactory(File sourceDirectories[], String matchString, DraftStatus minimalDraftStatus) {
        // initialize class based
        if (USE_OLD_HANDLEMAKE_CODE) {
            mainCache = new Map[DraftStatus.values().length];
            resolvedCache = new Map[DraftStatus.values().length];
            for (int i = 0; i < mainCache.length; ++i) {
                mainCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
                resolvedCache[i] = Collections.synchronizedMap(new LruMap<String, CLDRFile>(CACHE_LIMIT));
            }
        } else {
            // combinedCache=  Collections.synchronizedMap(new LruMap<CLDRCacheKey, CLDRFile>(CACHE_LIMIT));
            combinedCache = CacheBuilder.newBuilder().maximumSize(CACHE_LIMIT).build();
        }
        //
        this.sourceDirectories = sourceDirectories;
        this.minimalDraftStatus = minimalDraftStatus;
        Matcher m = PatternCache.get(matchString).matcher("");
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

    public static class NoSourceDirectoryException extends ICUUncheckedIOException {
        private static final long serialVersionUID = 1L;
        private final String localeName;

        public NoSourceDirectoryException(String localeName) {
            this.localeName = localeName;
        }

        @Override
        public String getMessage() {
            return "Unable to determine the source directory for locale " + localeName;
        }
    }

    /**
     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
     * use clone().
     */
    @SuppressWarnings("unchecked")
    public CLDRFile handleMake(String localeName, boolean resolved, DraftStatus minimalDraftStatus) {
        @SuppressWarnings("rawtypes")
        final Map mapToSynchronizeOn;
        final List<File> parentDirs = getSourceDirectoriesForLocale(localeName);
        /*
         *  Parameter check: parentDir being null means the source directory could not be found - throw exception here
         *  rather than running into a  NullPointerException when trying to create/store the cache key further down.
         */
        if (parentDirs == null) {
            // changed from IllegalArgumentException, which does't let us filter exceptions.
            throw new NoSourceDirectoryException(localeName);
        }
        final Object cacheKey;
        CLDRFile result; // result of the lookup / generation
        if (USE_OLD_HANDLEMAKE_CODE) {
            final Map<String, CLDRFile> cache = resolved ? resolvedCache[minimalDraftStatus.ordinal()] : mainCache[minimalDraftStatus.ordinal()];
            mapToSynchronizeOn = cache;
            cacheKey = localeName;
            result = cache.get(localeName);
        } else {
            // Use double-check idiom
            cacheKey = new CLDRCacheKey(localeName, resolved, minimalDraftStatus, parentDirs);
            //        result = cache.get(localeName);
            //  result=combinedCache.asMap().get(cacheKey);
            result = combinedCache.getIfPresent(cacheKey);
            mapToSynchronizeOn = combinedCache.asMap();
        }
        if (result != null) {
            if (DEBUG_SIMPLEFACTORY) {
                System.out.println("HandleMake:Returning cached result for locale " + localeName);
            }
            return result;
        }
//        synchronized (cache) {
        synchronized (mapToSynchronizeOn) {
            // Check cache twice to ensure that CLDRFile is only loaded once
            // even with multiple threads.
            //            result = cache.get(localeName);
            //     result=combinedCache.get(cacheKey);
            Object returned = mapToSynchronizeOn.get(cacheKey);
            if (returned instanceof CLDRFile) {
                result = (CLDRFile) returned;
            }
            if (result != null) {
                if (DEBUG_SIMPLEFACTORY) {
                    System.out.println("HandleMake:Returning cached result for locale " + localeName);
                }
                return result;
            }
            if (resolved) {
                result = new CLDRFile(makeResolvingSource(localeName, minimalDraftStatus));
            } else {
                if (parentDirs != null) {
                    if (DEBUG_SIMPLEFACTORY) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("HandleMake: Calling makeFile with locale: ");
                        sb.append(localeName);
                        sb.append(", parentDir: ");
                        sb.append(parentDirs);
                        sb.append(", DraftStatus: ");
                        sb.append(minimalDraftStatus);
                        System.out.println(sb.toString());
                    }
                    result = makeFile(localeName, parentDirs, minimalDraftStatus);
                    result.freeze();
                }
            }
            if (result != null) {
                mapToSynchronizeOn.put(cacheKey, result);
                //                combinedCache.put(cacheKey, result);
                //                cache.put(localeName, result);
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

    private static CLDRFile makeFromFile(List<File> dirs, String localeName, DraftStatus minimalDraftStatus) {
        return CLDRFile.loadFromFiles(dirs, localeName, minimalDraftStatus);
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

    public static CLDRFile makeFile(String localeName, List<File> dirs, CLDRFile.DraftStatus minimalDraftStatus) {
        CLDRFile file = makeFromFile(dirs, localeName, minimalDraftStatus);
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
    public List<File> getSourceDirectoriesForLocale(String localeName) {
        Builder<File> result = null;
        boolean isSupplemental = CLDRFile.isSupplementalName(localeName);
        for (File sourceDirectory : this.sourceDirectories) {
            if (isSupplemental) {
                sourceDirectory = new File(
                    sourceDirectory.getAbsolutePath().replace("incoming" + File.separator + "vetted" + File.separator, "common" + File.separator));
            }
            final File dir = isSupplemental ? new File(sourceDirectory, "../supplemental") : sourceDirectory;
            final File xmlFile = makeFileName(localeName, dir);
            if (xmlFile.canRead()) {
                if (result == null) {
                    result = ImmutableList.<File> builder();
                }
                result.add(dir);
            }
        }
        return result == null ? null : result.build();
    }

}