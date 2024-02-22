package org.unicode.cldr.web;

import java.io.File;
import java.time.Instant;
import java.util.logging.Logger;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.rdf.AbstractCache;
import org.unicode.cldr.rdf.MapAll;
import org.unicode.cldr.util.CLDRCacheDir;

class AbstractCacheManager {
    static final Logger logger = SurveyLog.forClass(AbstractCacheManager.class);
    static final int DEFAULT_DAYS = 5;

    static final AbstractCacheManager getInstance() {
        return AbstractCacheHelper.INSTANCE;
    }

    static final class AbstractCacheHelper {
        static final AbstractCacheManager INSTANCE = new AbstractCacheManager();
    }

    final String resourceUriForXpath(String xpath) {
        if (!setup) {
            return null;
        }

        return cache.get(xpath);
    }

    public boolean setup = false;
    private AbstractCache cache = null;
    private CLDRCacheDir cacheDir;

    /**
     * Set the CLDR Home so we can setup.
     *
     * @param cldrHome
     */
    public void setup() {
        this.cacheDir = CLDRCacheDir.getInstance(CLDRCacheDir.CacheType.abstracts);
        final File subroot = cacheDir.getEmptyDir();
        AbstractCache c = new AbstractCache(subroot);
        Instant d = c.load();
        // Expire the URL mapping every 90 days.
        final Instant latestGoodInstant = cacheDir.getType().getLatestGoodInstant(DEFAULT_DAYS);
        // -- cf. "...ready for requests..." in SurveyMain called later but printed sooner
        logger.info("AbstractCacheManager: Loaded abstracts (last calculated at " + d + ")");
        if (d == null || d.isBefore(latestGoodInstant)) {
            System.out.println("Will reload abstracts: " + d + " with expiry " + latestGoodInstant);

            int count;
            try {
                // does not delete old entries.
                count = new MapAll().addEntries(c);
                System.out.println("Loaded entry count: " + count);
                c.store(); // overwrite old cache
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.println("Error loading Abstract maps");
            }
        }
        cache = c;
        setup = true;
    }

    public static final void main(String args[]) {
        getInstance().setup();
    }
}
