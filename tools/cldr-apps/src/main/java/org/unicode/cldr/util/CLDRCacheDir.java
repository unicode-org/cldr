package org.unicode.cldr.util;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.unicode.cldr.web.SurveyLog;

/** Central management for cache files. */
public class CLDRCacheDir {
    static final Logger logger = SurveyLog.forClass(CLDRCacheDir.class);
    private static final String CACHE_SUBDIR = ".cache";

    /** All users of the cache must have an enum entry here */
    public enum CacheType {
        abstracts("Article excerpts from DBPedia"),
        xmlCache("Precomputed XML metadata and validity"),
        sandbox(
                "Temporary locales that may be used for user experimentation, data may not be persisted."),
        urlmap("URL-to-Subtype mapping");

        private String description;

        public String getDescription() {
            return description;
        }

        /** */
        CacheType(String description) {
            this.description = description;
        }

        /**
         * Get the 'latest good' date before which a cached item is considered stale. For example,
         * if the expiry is 3 days, then the Instant will reflect a date 3 days ago. For some item,
         * if "item.isBefore(latestGoodInstant)" then the item is stale.
         *
         * <p>This is overridable by a property, such as CLDR_ABSTRACT_EXPIRE_DAYS
         *
         * @param expireDays the default days if the property is not set
         * @return
         */
        public Instant getLatestGoodInstant(int expireDays) {
            return CLDRCacheDir.getLatestGoodInstant(getExpiryProperty(), expireDays);
        }

        private String getExpiryProperty() {
            return "CLDR_" + name().toUpperCase() + "_EXPIRE_DAYS";
        }
    }

    /**
     * Get the 'latest good' date before which a cached item is considered stale. For example, if
     * the expiry is 3 days, then the Instant will reflect a date 3 days ago. For some item, if
     * "item.isBefore(latestGoodInstant)" then the item is stale.
     *
     * @param propName property that can override the expiry
     * @param expireDays the default days if the property is not set
     * @return
     */
    static Instant getLatestGoodInstant(final String propName, int expireDays) {
        Instant now = Instant.now();
        Instant latestGood =
                now.minus(
                        CLDRConfig.getInstance().getProperty(propName, expireDays),
                        ChronoUnit.DAYS);
        return latestGood;
    }

    private final CacheType type;

    private File file;

    CLDRCacheDir(CacheType type) {
        this.type = type;
        CACHE_HELPER.assertOK(); // throw any exception needed
        this.file = getFile(this.type);
    }

    /**
     * Construct a new CLDRCacheDir handle using the specified type. This can be called from a
     * servlet environment or from within unit tests (for unit testing some submodule), however, if
     * called in the servlet environment it should be called after
     *
     * @param type
     * @return
     */
    public static CLDRCacheDir getInstance(CacheType type) {
        return new CLDRCacheDir(type);
    }

    private static File getFile(CacheType type) {
        return new File(CACHE_HELPER.rootDir, type.name());
    }

    /**
     * Get the File pertaining to this cache entry.
     *
     * @return
     */
    public File getDir() {
        return file;
    }

    /**
     * Return the file, as an empty dir. This is the function most callers will want to use.
     *
     * @return
     */
    public File getEmptyDir() {
        getDir().mkdirs();
        return getDir();
    }

    public CacheType getType() {
        return type;
    }

    /**
     * Delete the specified cache entry.
     *
     * @throws IOException
     */
    void remove() throws IOException {
        FileUtils.deleteDirectory(getDir());
    }

    /**
     * Remove all cache entries. Leaves the cache directory itself.
     *
     * @throws IOException
     */
    static void removeAll() throws IOException {
        for (final CacheType t : CacheType.values()) {
            FileUtils.deleteDirectory(getFile(t));
        }
    }

    /**
     * Bill Pugh style singleton for setting up caches. This function needs to always "succeed", but
     * will defer errors into the 'err' field.
     */
    static final class CacheHelper {
        File rootDir = null;
        Throwable err = null;

        CacheHelper() {
            try {
                // Make sure CLDRConfig is loaded.
                final CLDRConfig config = CLDRConfig.getInstance();
                if (config instanceof CLDRConfigImpl) {
                    File homeFile = CLDRConfigImpl.getInstance().getHomeFile();
                    if (homeFile == null) {
                        err =
                                new IllegalArgumentException(
                                        "CLDRConfigImpl present but getHomeFile() returned null.");
                    } else {
                        rootDir = new File(homeFile, CACHE_SUBDIR);
                    }
                } else {
                    // CLDRConfigImpl isn't around to give us a proper home file, so just use a
                    // temporary directory.
                    // We are probably not within a servlet environment.
                    // In any event, these are only cache files.
                    rootDir = Files.createTempDir();
                }
                logger.info("CLDRCacheDir: " + rootDir.getAbsolutePath());
                setup();
            } catch (Throwable t) {
                err = t;
            }
        }

        void assertOK() throws IllegalArgumentException {
            if (err != null) {
                throw new IllegalArgumentException("Not setup OK", err);
            }
        }

        /**
         * Make sure the cache directory is setup.
         *
         * @throws IOException
         */
        void setup() throws IOException {
            if (rootDir != null) {
                if (!rootDir.isDirectory()) {
                    System.err.println("Creating cache directory " + rootDir.getAbsolutePath());
                    rootDir.mkdirs();
                }
                File tag = new File(rootDir, "CACHEDIR.TAG");
                if (!tag.exists()) {
                    System.err.println("Writing cache tag " + tag.getAbsolutePath());
                    Files.asCharSink(tag, StandardCharsets.UTF_8)
                            .write(
                                    "Signature: 8a477f597d28d172789f06886806bc55\n"
                                            + "# This file is a cache directory tag created by the CLDR Survey Tool <https://unicode.org/cldr>\n"
                                            + "# For information about cache directory tags, see:\n"
                                            + "#   https://bford.info/cachedir/\n"
                                            + "");
                }
            } else {
                throw new NullPointerException(
                        "CLDRCacheDir could not calculate rootDir. Make sure caches are not accessed prior to servlet initialization.");
            }
        }
    }

    static final CacheHelper CACHE_HELPER = new CacheHelper();

    public static void main(String args[]) {
        new CLDRCacheDir(CacheType.abstracts);
    }
}
