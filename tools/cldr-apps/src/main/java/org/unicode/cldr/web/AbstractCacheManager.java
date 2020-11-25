package org.unicode.cldr.web;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.rdf.AbstractCache;
import org.unicode.cldr.rdf.MapAll;
import org.unicode.cldr.util.CLDRConfig;

class AbstractCacheManager {
    static final AbstractCacheManager getInstance() {
        return AbstractCacheHelper.INSTANCE;
    }
    static final class AbstractCacheHelper {
        static final AbstractCacheManager INSTANCE = new AbstractCacheManager();
    }

    final String resourceUriForXpath(String xpath) {
        if(!setup) {
            return null;
        }

        return cache.get(xpath);
	}

    public boolean setup = false;
    private AbstractCache cache = null;

    /**
     * Set the CLDR Home so we can setup.
     * @param cldrHome
     */
    public void setHome(File cldrHome) {
        final File subroot = new File(cldrHome, "abstracts");
        subroot.mkdirs();
        AbstractCache c = new AbstractCache(subroot);
        Instant d = c.load();
        Instant now = Instant.now();
        // Expire the URL mapping every 90 days.
        Instant expireIfBefore = now.minus(CLDRConfig.getInstance().getProperty("CLDR_ABSTRACT_EXPIRE_DAYS", 15), ChronoUnit.DAYS);
        System.err.println("AbstractCacheManager: Loaded abstracts (last calculated at " + d+")");
        if(d == null || d.isBefore(expireIfBefore)) {
            System.out.println("Will reload abstracts: " + d + " with expiry " + expireIfBefore);

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
        getInstance().setHome(new File(args[0]));
    }
}