// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Files;

/**
 * This is a cache for the XPATH to URI mapping
 * It caches using 1 property file in the specified directory.
 * @author srl295
 */
public class AbstractCache {
    // This is equivalent to SurveyLog.forClass()
    static final Logger logger = Logger.getLogger(AbstractCache.class.getName());
    private static final String XPATH_TO_RESOURCE_FILE = "xpath-to-resource.properties";

    /**
     * Add an AbstractResource to the cache. Remember to call store() to
     * persist the cache.
     * @param xpath
     * @param ar
     * @return true if some value was already there, false if this was a new value
     */
    public boolean add(String xpath, String uri) {
        return (xpathToResource.put(xpath, uri) != null);
    }

    /**
     * Get the xpath mapping, or null
     * @param xpath
     * @return
     */
    public String get(String xpath) {
        return (String)xpathToResource.get(xpath);
    }

    private final File root;
    private final File xpathToResourceFile;


    Properties xpathToResource = new Properties();

    /**
     * Initialize the abstract cache with a certain root location
     * @param root
     */
    public AbstractCache(File root) {
        this.root = root;
        xpathToResourceFile = new File(root, XPATH_TO_RESOURCE_FILE);
        load();
    }

    /**
     * Load (or reload) the abstract cache.
     * On failure, will clear this cache.
     */
    public Instant load() {
        final String simpleName = this.getClass().getSimpleName();
        synchronized(this) {
            try(
                Reader xp2res = Files.newReader(xpathToResourceFile, StandardCharsets.UTF_8);
            ) {
                xpathToResource.load(xp2res);
                logger.fine("# " + simpleName + " read " + root.getAbsolutePath() + " count: " + size());
                return Instant.ofEpochMilli(xpathToResourceFile.lastModified());
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Could not read files in " + root.getAbsolutePath());
                // Full stacktrace at a higher trace level
                logger.log(Level.FINE, "Could not read " + root.getAbsolutePath() + " - " + ioe.getMessage());
                xpathToResource.clear();
                return null;
            }
        }
    }

    /**
     * Write out the cache
     */
    public void store() {
        final String simpleName = this.getClass().getSimpleName();
        synchronized(this) {
            root.mkdirs();
            try(
                Writer xp2res = Files.newWriter(xpathToResourceFile, StandardCharsets.UTF_8);
            ) {
                xpathToResource.store(xp2res, "Written by " + simpleName);
                logger.info("# " + simpleName + " wrote to " + root.getAbsolutePath());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                logger.log(Level.SEVERE, "Could not write files in " + root.getAbsolutePath(), ioe);
            }
        }
    }

	public int size() {
		return xpathToResource.size();
	}
}
