package org.unicode.cldr.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.test.CheckCLDR.Phase;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * Basic information about the CLDR environment.
 * Use CLDRConfig.getInstance() to create your instance.
 *
 * Special notes:
 * - Within the Survey Tool, a special subclass of this class named CLDRConfigImpl is used instead,
 * which see.
 * - Within unit tests, -DCLDR_ENVIRONMENT=UNITTEST is set, which prevents the use of CLDRConfigImpl
 */
public class CLDRConfig extends Properties {
    public static boolean SKIP_SEED = System.getProperty("CLDR_SKIP_SEED") != null;
    private static final long serialVersionUID = -2605254975303398336L;
    public static boolean DEBUG = false;
    /**
     * This is the special implementation which will be used, i.e. CLDRConfigImpl
     */
    public static final String SUBCLASS = CLDRConfig.class.getName() + "Impl";

    /**
     * What environment is CLDR in?
     */
    public enum Environment {
        LOCAL, // < == unknown.
        SMOKETEST, // staging (SurveyTool) area
        PRODUCTION, // production (SurveyTool) server!
        UNITTEST // unit test setting
    }

    public static final class CLDRConfigHelper {
        private static CLDRConfig make() {
            CLDRConfig instance = null;
            final String env = System.getProperty("CLDR_ENVIRONMENT");
            if (env != null && env.equals(Environment.UNITTEST.name())) {
                // For unittests, skip the following
                if (DEBUG) {
                    System.err.println("-DCLDR_ENVIRONMENT=" + env + " - not loading " + SUBCLASS);
                }
            } else {
                // This is the branch for SurveyTool
                try {
                    // System.err.println("Attempting to new up a " + SUBCLASS);
                    instance = (CLDRConfig) (Class.forName(SUBCLASS).newInstance());

                    if (instance != null) {
                        System.err.println("Using CLDRConfig: " + instance.toString() + " - "
                            + instance.getClass().getName());
                    } else {
                        if (DEBUG) {
                            // Probably occurred because ( config.getEnvironment() == Environment.UNITTEST )
                            // see CLDRConfigImpl
                            System.err.println("Note: CLDRConfig Subclass " +
                                SUBCLASS + ".newInstance() returned NULL " +
                                "( this is OK if we aren't inside the SurveyTool's web server )");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Expected - when not under cldr-apps, this class doesn't exist.
                } catch (InstantiationException | IllegalAccessException e) {
                    // TODO: log a useful message
                }
            }
            if (instance == null) {
                // this is the "normal" branch for tools and such
                instance = new CLDRConfig();
                CldrUtility.checkValidDirectory(instance.getProperty("CLDR_DIR"),
                    "You have to set -DCLDR_DIR=<validdirectory>");
            }
            return instance;
        }

        static final CLDRConfig SINGLETON = make();
    }

    /**
     * Main getter for the singleton CLDRConfig.
     * @return
     */
    public static CLDRConfig getInstance() {
        return CLDRConfigHelper.SINGLETON;
    }

    String initStack = null;

    protected CLDRConfig() {
        initStack = StackTracker.currentStack();
    }

    /**
     * This returns the stacktrace of the first caller to getInstance(), for debugging.
     * @return
     */
    public String getInitStack() {
        return initStack;
    }

    private Phase phase = null; // default

    private LoadingCache<String, CLDRFile> cldrFileResolvedCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .build(
            new CacheLoader<String, CLDRFile>() {
                @Override
                public CLDRFile load(String locale) {
                    return getFullCldrFactory().make(locale, true);
                }
            });

    // Unresolved CLDRFiles are smaller than resolved, so we can cache more of them safely.
    private LoadingCache<String, CLDRFile> cldrFileUnresolvedCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(
            new CacheLoader<String, CLDRFile>() {
                @Override
                public CLDRFile load(String locale) {
                    return getFullCldrFactory().make(locale, false);
                }
            });
    private TestLog testLog = null;

    // base level
    public TestLog setTestLog(TestLog log) {
        testLog = log;
        return log;
    }

    // for calling "run"
    public TestFmwk setTestLog(TestFmwk log) {
        testLog = log;
        return log;
    }

    protected void logln(String msg) {
        if (testLog != null) {
            testLog.logln(msg);
        } else {
            System.out.println(msg);
            System.out.flush();
        }
    }

    private static final class SupplementalDataInfoHelper {
        static final SupplementalDataInfo SINGLETON = SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
    }

    public SupplementalDataInfo getSupplementalDataInfo() {
        // Note: overridden in subclass.
        return SupplementalDataInfoHelper.SINGLETON;
    }

    private static final class CoverageInfoHelper {
        static final CoverageInfo SINGLETON = new CoverageInfo(getInstance().getSupplementalDataInfo());
    }

    public final CoverageInfo getCoverageInfo() {
        return CoverageInfoHelper.SINGLETON;
    }

    private static final class CldrFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
    }

    public final Factory getCldrFactory() {
        return CldrFactoryHelper.SINGLETON;
    }

    private static final class ExemplarsFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.EXEMPLARS_DIRECTORY, ".*");
    }

    public final Factory getExemplarsFactory() {
        return ExemplarsFactoryHelper.SINGLETON;
    }

    private static final class CollationFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.COLLATION_DIRECTORY, ".*");
    }

    public final Factory getCollationFactory() {
        return CollationFactoryHelper.SINGLETON;
    }

    private static final class RBNFFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.RBNF_DIRECTORY, ".*");
    }

    public final Factory getRBNFFactory() {
        return RBNFFactoryHelper.SINGLETON;
    }

    private static final class AnnotationsFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
    }

    public Factory getAnnotationsFactory() {
        return AnnotationsFactoryHelper.SINGLETON;
    }

    private static final class SubdivisionsFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.SUBDIVISIONS_DIRECTORY, ".*");
    }

    public final Factory getSubdivisionFactory() {
        return SubdivisionsFactoryHelper.SINGLETON;
    }

    private static final class MainAndAnnotationsFactoryHelper {
        private static final File[] paths = {
            new File(CLDRPaths.MAIN_DIRECTORY),
            new File(CLDRPaths.ANNOTATIONS_DIRECTORY) };
        static final Factory SINGLETON = SimpleFactory.make(paths, ".*");
    }

    public final Factory getMainAndAnnotationsFactory() {
        return MainAndAnnotationsFactoryHelper.SINGLETON;
    }

    private static final class CommonSeedExemplarsFactoryHelper {
        static final Factory SINGLETON = SimpleFactory.make(getInstance().addStandardSubdirectories(CLDR_DATA_DIRECTORIES), ".*");
    }

    public final Factory getCommonSeedExemplarsFactory() {
        return CommonSeedExemplarsFactoryHelper.SINGLETON;
    }

    private static final class CommonAndSeedAndMainAndAnnotationsFactoryHelper {
        private static final File[] paths = {
            new File(CLDRPaths.MAIN_DIRECTORY),
            new File(CLDRPaths.ANNOTATIONS_DIRECTORY),
            SKIP_SEED ? null : new File(CLDRPaths.SEED_DIRECTORY),
                SKIP_SEED ? null : new File(CLDRPaths.SEED_ANNOTATIONS_DIRECTORY)
        };
        static final Factory SINGLETON = SimpleFactory.make(paths, ".*");
    }

    public final Factory getCommonAndSeedAndMainAndAnnotationsFactory() {
        return CommonAndSeedAndMainAndAnnotationsFactoryHelper.SINGLETON;
    }

    private static final class FullCldrFactoryHelper {
        private static final File[] paths = {
            new File(CLDRPaths.MAIN_DIRECTORY),
            SKIP_SEED ? null : new File(CLDRPaths.SEED_DIRECTORY)};
        static final Factory SINGLETON = SimpleFactory.make(paths, ".*");
    }

    public final Factory getFullCldrFactory() {
        return FullCldrFactoryHelper.SINGLETON;
    }

    private static final class SupplementalFactoryHelper {
        static final Factory SINGLETON = Factory.make(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY, ".*");
    }

    public final Factory getSupplementalFactory() {
        return SupplementalFactoryHelper.SINGLETON;
    }

    public CLDRFile getEnglish() {
        return getCLDRFile("en", true);
    }

    public CLDRFile getCLDRFile(String locale, boolean resolved) {
        return resolved ? cldrFileResolvedCache.getUnchecked(locale) : cldrFileUnresolvedCache.getUnchecked(locale);
    }

    public CLDRFile getRoot() {
        return getCLDRFile("root", true);
    }

    private static final class CollatorRootHelper {
        static final RuleBasedCollator SINGLETON = make();

        private static final RuleBasedCollator make() {
            RuleBasedCollator colRoot;

            CLDRFile root = getInstance().getCollationFactory().make("root", false);
            String rules = root.getStringValue("//ldml/collations/collation[@type=\"emoji\"][@visibility=\"external\"]/cr");
            try {
                colRoot = new RuleBasedCollator(rules);
            } catch (Exception e) {
                colRoot = (RuleBasedCollator) getInstance().getCollator();
                return colRoot;
            }
            colRoot.setStrength(Collator.IDENTICAL);
            colRoot.setNumericCollation(true);
            colRoot.freeze();
            return colRoot;
        }
    }
    public final Collator getCollatorRoot() {
        return CollatorRootHelper.SINGLETON;
    }

    @SuppressWarnings("unchecked")
    public final Comparator<String> getComparatorRoot() {
        return (Comparator)(getCollatorRoot());
    }

    private static final class CollatorHelper {
        static final Collator EMOJI_COLLATOR = makeEmojiCollator();
        private static final Collator makeEmojiCollator() {
            final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.forLanguageTag("en-u-co-emoji"));
            col.setStrength(Collator.IDENTICAL);
            col.setNumericCollation(true);
            col.freeze();
            return col;
        }

        static final Collator ROOT_NUMERIC = makeRootNumeric();

        private static final Collator makeRootNumeric() {
            RuleBasedCollator _ROOT_COL = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
            _ROOT_COL.setNumericCollation(true);
            _ROOT_COL.freeze();
            return _ROOT_COL;
        }
    }
    public Collator getCollator() {
        return CollatorHelper.EMOJI_COLLATOR;
    }

    public Collator getRootNumeric() {
        return CollatorHelper.ROOT_NUMERIC;
    }

    public synchronized Phase getPhase() {
        if (phase == null) {
            if (getEnvironment() == Environment.UNITTEST) {
                phase = Phase.BUILD;
            } else {
                phase = Phase.SUBMISSION;
            }
        }
        return phase;
    }

    @Override
    public String getProperty(String key, String d) {
        String result = getProperty(key);
        if (result == null) return d;
        return result;
    }

    private Set<String> shown = new HashSet<>();

    private Map<String, String> localSet = null;

    @Override
    public String get(Object key) {
        return getProperty(key.toString());
    }

    @Override
    public String getProperty(String key) {
        String result = null;
        if (localSet != null) {
            result = localSet.get(key);
        }
        if (result == null) {
            result = System.getProperty(key);
        }
        if (result == null) {
            result = System.getProperty(key.toUpperCase(Locale.ENGLISH));
        }
        if (result == null) {
            result = System.getProperty(key.toLowerCase(Locale.ENGLISH));
        }
        if (result == null) {
            result = System.getenv(key);
        }
        if (DEBUG && !shown.contains(key)) {
            logln("-D" + key + "=" + result);
            shown.add(key);
        }
        return result;
    }

    private Environment curEnvironment = null;

    public Environment getEnvironment() {
        if (curEnvironment == null) {
            String envString = getProperty("CLDR_ENVIRONMENT");
            if (envString != null) {
                curEnvironment = Environment.valueOf(envString.trim());
            }
            if (curEnvironment == null) {
                curEnvironment = getDefaultEnvironment();
            }
        }
        return curEnvironment;
    }

    /**
     * If no environment is defined, what is the default?
     * @return
     */
    protected Environment getDefaultEnvironment() {
        return Environment.LOCAL;
    }

    public void setEnvironment(Environment environment) {
        curEnvironment = environment;
    }

    /**
     * For test use only. Will throw an exception in non test environments.
     * @param k
     * @param v
     * @return
     */
    @Override
    public Object setProperty(String k, String v) {
        if (getEnvironment() != Environment.UNITTEST) {
            throw new InternalError("setProperty() only valid in UNITTEST Environment.");
        }
        if (localSet == null) {
            localSet = new ConcurrentHashMap<>();
        }
        shown.remove(k); // show it again with -D
        return localSet.put(k, v);
    }

    @Override
    public Object put(Object k, Object v) {
        return setProperty(k.toString(), v.toString());
    }

    /**
     * Return true if the value indicates 'true'
     * @param k key
     * @param defVal default value
     * @return
     */
    public boolean getProperty(String k, boolean defVal) {
        String val = getProperty(k, defVal ? "true" : null);
        if (val == null) {
            return false;
        } else {
            val = val.trim().toLowerCase();
            return (val.equals("true") || val.equals("t") || val.equals("yes") || val.equals("y"));
        }
    }

    /**
     * Return a numeric property
     * @param k key
     * @param defVal default value
     * @return
     */
    public int getProperty(String k, int defVal) {
        String val = getProperty(k, Integer.toString(defVal));
        if (val == null) {
            return defVal;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                return defVal;
            }
        }
    }

    private static class FileWrapper {
        private File cldrDir = null;
        private FileWrapper() {
            String dir = getInstance().getProperty("CLDR_DIR", null);
            if (dir != null) {
                cldrDir = new File(dir);
            } else {
                cldrDir = null;
            }
        }
        public File getCldrDir() {
            return this.cldrDir;
        }
        // singleton
        private static FileWrapper fileWrapperInstance = new FileWrapper();
        public static FileWrapper getFileWrapperInstance() {
            return fileWrapperInstance;
        }
    }

    public File getCldrBaseDirectory() {
        return FileWrapper.getFileWrapperInstance().getCldrDir();
    }

    /**
     * Get all CLDR XML files in the CLDR base directory.
     * @return
     */
    public Set<File> getAllCLDRFilesEndingWith(final String suffix) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(suffix) && !isJunkFile(name); // skip junk and backup files
            }
        };
        final File dir = getCldrBaseDirectory();
        Set<File> list;
        list = getCLDRFilesMatching(filter, dir);
        return list;
    }

    /**
     * Return all CLDR data files matching this filter
     * @param filter matching filter
     * @param baseDir base directory, see {@link #getCldrBaseDirectory()}
     * @return set of files
     */
    public Set<File> getCLDRFilesMatching(FilenameFilter filter, final File baseDir) {
        Set<File> list;
        list = new LinkedHashSet<>();
        for (String subdir : getCLDRDataDirectories()) {
            getFilesRecursively(new File(baseDir, subdir), filter, list);
        }
        return list;
    }

    /**
     * TODO: better place for these constants?
     */
    private static final String COMMON_DIR = "common";
    /**
     * TODO: better place for these constants?
     */
    private static final String EXEMPLARS_DIR = "exemplars";
    /**
     * TODO: better place for these constants?
     */
    private static final String SEED_DIR = "seed";
    /**
     * TODO: better place for these constants?
     */
    private static final String KEYBOARDS_DIR = "keyboards";
    private static final String MAIN_DIR = "main";
    private static final String ANNOTATIONS_DIR = "annotations";
    private static final String SUBDIVISIONS_DIR = "subdivisions";

    /**
     * TODO: better place for these constants?
     */
    private static final String CLDR_DATA_DIRECTORIES[] = { COMMON_DIR, SEED_DIR, KEYBOARDS_DIR, EXEMPLARS_DIR };
    private static final ImmutableSet<String> STANDARD_SUBDIRS = ImmutableSet.of(MAIN_DIR, ANNOTATIONS_DIR, SUBDIVISIONS_DIR);

    /**
     * Get a list of CLDR directories containing actual data
     * @return an iterable containing the names of all CLDR data subdirectories
     */
    public static Iterable<String> getCLDRDataDirectories() {
        return Arrays.asList(CLDR_DATA_DIRECTORIES);
    }

    /**
     * Given comma separated list "common" or "common,main" return a list of actual files.
     * Adds subdirectories in STANDARD_SUBDIRS as necessary.
     */
    public File[] getCLDRDataDirectories(String list) {
        final File dir = getCldrBaseDirectory();
        String stubs[] = list.split(",");
        File[] ret = new File[stubs.length];
        for (int i = 0; i < stubs.length; i++) {
            ret[i] = new File(dir, stubs[i]);
        }
        return ret;
    }

    /**
     * Add subdirectories to file list as needed, from STANDARD_SUBDIRS.
     * <ul><li>map "common","seed" -> "common/main", "seed/main"
     * <li>but common/main -> common/main
     * </ul>
     */
    public File[] addStandardSubdirectories(String... base) {
        return addStandardSubdirectories(fileArrayFromStringArray(getCldrBaseDirectory(), base));
    }

    public File[] addStandardSubdirectories(File... base) {
        List<File> ret = new ArrayList<>();
        //File[] ret = new File[base.length * 2];
        for (int i = 0; i < base.length; i++) {
            File baseFile = base[i];
            String name = baseFile.getName();
            if (STANDARD_SUBDIRS.contains(name)) {
                ret.add(baseFile);
            } else {
                for (String sub : STANDARD_SUBDIRS) {
                    addIfExists(ret, baseFile, sub);
                }
            }
        }
        return ret.toArray(new File[ret.size()]);
    }

    public static File[] fileArrayFromStringArray(File dir, String... subdirNames) {
        File[] fileList = new File[subdirNames.length];
        int i = 0;
        for (String item : subdirNames) {
            fileList[i++] = new File(dir, item);
        }
        return fileList;
    }

    private static void addIfExists(List<File> ret, File baseFile, String sub) {
        File file = new File(baseFile, sub);
        if (file.exists()) {
            ret.add(file);
        }
    }

    /**
     * Utility function. Recursively add to a list of files. Skips ".svn" and junk directories.
     * @param directory base directory
     * @param filter filter to restrict files added
     * @param toAddTo set to add to
     * @return returns toAddTo.
     */
    public Set<File> getFilesRecursively(File directory, FilenameFilter filter, Set<File> toAddTo) {
        File files[] = directory.listFiles();
        if (files != null) {
            for (File subfile : files) {
                if (subfile.isDirectory()) {
                    if (!isJunkFile(subfile.getName())) {
                        getFilesRecursively(subfile, filter, toAddTo);
                    }
                } else if (filter.accept(directory, subfile.getName())) {
                    toAddTo.add(subfile);
                }
            }
        }
        return toAddTo;
    }

    /**
     * Is the filename junk?  (subversion, backup, etc)
     * @param name
     * @return
     */
    public static final boolean isJunkFile(String name) {
        return name.startsWith(".") || (name.startsWith("#")); // Skip:  .svn, .BACKUP,  #backup# files.
    }

    /**
     * Get the value of the debug setting for the calling class; assuming that no debugging is wanted if the property
     * value cannot be found
     * @param callingClass
     * @return
     * @see {@link #getDebugSettingsFor(Class, boolean)}
     */
    public boolean getDebugSettingsFor(Class<?> callingClass) {
        return getDebugSettingsFor(callingClass, false);
    }

    /**
     * Get the debug settings (whether debugging is enabled for the calling class; This will look for a property corresponding
     * to the canonical classname +".debug"; if that property cannot be found, the default value will be returned.
     * @param callingClass
     * @param defaultValue
     * @return
     */
    public boolean getDebugSettingsFor(Class<?> callingClass, boolean defaultValue) {
        // avoid NPE
        if (callingClass == null) {
            return defaultValue;
        }
        return getProperty(callingClass.getCanonicalName() + ".debug", defaultValue);
    }

    /**
     * Get the URL generator for "general purpose" (non chart) use.
     * @return
     */
    public CLDRURLS urls() {
        if (urls == null) {
            synchronized (this) {
                urls = internalGetUrls();
            }
        }
        return urls;
    }

    /**
     * Get the URL generator for "absolute" (chart, email) use.
     * By default, this is the same as urls.
     */
    public CLDRURLS absoluteUrls() {
        if (absoluteUrls == null) {
            synchronized (this) {
                absoluteUrls = internalGetAbsoluteUrls();
            }
        }
        return absoluteUrls;
    }

    /**
     * Probably would not need to override this.
     */
    protected CLDRURLS internalGetAbsoluteUrls() {
        return new StaticCLDRURLS(this.getProperty(CLDRURLS.CLDR_SURVEY_BASE, CLDRURLS.DEFAULT_BASE));
    }

    /**
     * Override this to provide a different URL source for non-absolute URLs.
     */
    protected CLDRURLS internalGetUrls() {
        return absoluteUrls();
    }

    private CLDRURLS urls = null;
    private CLDRURLS absoluteUrls = null;

    public boolean isCldrVersionBefore(int... version) {
        return getEnglish().getDtdVersionInfo()
            .compareTo(getVersion(version)) < 0;
    }

    public static VersionInfo getVersion(int... versionInput) {
        int[] version = new int[4];
        for (int i = 0; i < versionInput.length; ++i) {
            version[i] = versionInput[i];
        }
        return VersionInfo.getInstance(version[0], version[1], version[2],
            version[3]);
    }
}
