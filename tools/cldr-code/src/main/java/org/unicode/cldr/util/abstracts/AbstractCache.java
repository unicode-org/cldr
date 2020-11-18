// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.util.abstracts;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;

/**
 * This is a caching implementation of AbstractProvider.
 * It caches using two property files in the specified directory.
 * @author srl295
 */
public class AbstractCache implements AbstractProvider {

    private AbstractResult forCacheEntry(String res, String cacheEntry) {
        // <date>;<abstract>
        final int semi = cacheEntry.indexOf(';');
        final Instant d = Instant.parse(cacheEntry.substring(0, semi));
        final String abs = cacheEntry.substring(semi+1);
        return new AbstractResult(d, res, abs);
    }
    
    private String toCacheEntry(AbstractResult ar) {
        return ar.getContentDate().toString() + ";" + ar.getContent();
    }
    
    /**
     * Add an AbstractResource to the cache. Remember to call store() to 
     * persist the cache.
     * @param xpath
     * @param ar 
     */
    public void add(String xpath, AbstractResult ar) {
        xpathToResource.put(xpath, ar.getResourceUri());
        resourceToAbstract.put(ar.getResourceUri(), toCacheEntry(ar));
    }
    
    static final class AbstractCacheHelper {
        static final AbstractCache INSTANCE = new AbstractCache(new File(CLDRConfig.getInstance().getCldrBaseDirectory(),
                "tools/cldr-rdf/abstracts"));
    }
    public static final AbstractCache getInstance() {
        return AbstractCacheHelper.INSTANCE;
    }
    
    private final File root;
    private final File xpathToResourceFile;
    private final File resourceToAbstractFile;
   
    @Override
    public boolean matches(String xpath) {
        return xpathToResource.contains(xpath);
    }

    @Override
    public AbstractResult abstractFor(String xpath) {
        final String res = (String)xpathToResource.get(xpath);
        if(res == null) return null;
        final String cacheEntry = (String)resourceToAbstract.get(res);
        if(cacheEntry == null) return null;
        return forCacheEntry(res, cacheEntry);
    }
    
    Properties xpathToResource = new Properties();
    Properties resourceToAbstract = new Properties();
    
    AbstractCache(File root) {
        this.root = root;
        xpathToResourceFile = new File(root, "xpath-to-resource.properties");
        resourceToAbstractFile = new File(root, "resource-to-abstract.properties");
        load();
    }
    
    /**
     * Load (or reload) the abstract cache
     */
    public void load() {
        synchronized(this) {
            try(
                Reader xp2res = Files.newReader(xpathToResourceFile, StandardCharsets.UTF_8);
                Reader res2abs = Files.newReader(resourceToAbstractFile, StandardCharsets.UTF_8);
            ) {
                xpathToResource.load(xp2res);
                resourceToAbstract.load(res2abs);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.err.println("Could not read files in " + root.getAbsolutePath() + " = " + ioe);
                xpathToResource.clear();
                resourceToAbstract.clear();
            }
        }
    }
    
    /**
     * Write out the cache
     */
    public void store() {
        synchronized(this) {
            root.mkdirs();
            try(
                Writer xp2res = Files.newWriter(xpathToResourceFile, StandardCharsets.UTF_8);
                Writer res2abs = Files.newWriter(resourceToAbstractFile, StandardCharsets.UTF_8);
            ) {
                final String simpleName = this.getClass().getSimpleName();
                xpathToResource.store(xp2res, "Written by " + simpleName);
                resourceToAbstract.store(res2abs, "Written by " + simpleName);
                System.err.println("# " + simpleName + " wrote to " + root.getAbsolutePath());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.err.println("Could not write files in " + root.getAbsolutePath() + " = " + ioe);
            }
        }
    }    
}
