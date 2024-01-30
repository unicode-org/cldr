package org.unicode.cldr.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.XMLSource;

/** Cache for on-disk immutable data. */
public class DiskDataCache {
    static final Logger logger = Logger.getLogger(DiskDataCache.class.getSimpleName());

    private final Factory factory;
    private final CLDRFile english;
    private final PathHeader.Factory phf;

    /** this is the immutable cousin of STFactory.PerLocaleData, for the on-disk data */
    class DiskDataEntry {
        final CLDRLocale locale;
        final XMLSource diskData;
        final CLDRFile diskFile;
        final Set<String> pathsForFile;

        public DiskDataEntry(CLDRLocale locale) {
            this.locale = locale;
            diskData = factory.makeSource(locale.getBaseName()).freeze();
            diskFile = factory.make(locale.getBaseName(), true).freeze();
            pathsForFile = getPathHeaderFactory().pathsForFile(diskFile);
        }
    }

    public DiskDataCache(Factory f, CLDRFile english) {
        this.factory = f;
        this.english = english;
        this.phf = PathHeader.getFactory(english);
    }

    public PathHeader.Factory getPathHeaderFactory() {
        return phf;
    }

    private LoadingCache<CLDRLocale, DiskDataEntry> cache =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<CLDRLocale, DiskDataEntry>() {

                                @Override
                                public DiskDataEntry load(CLDRLocale l) throws Exception {
                                    return new DiskDataEntry(l);
                                }
                            });

    public DiskDataEntry get(CLDRLocale locale) {
        logger.fine(() -> "Loading " + locale);
        try {
            return cache.get(locale);
        } catch (ExecutionException e) {
            SurveyLog.logException(logger, e, "Trying to construct " + locale);
            SurveyMain.busted("Loading " + locale, e);
            return null; /* notreached */
        }
    }
}
