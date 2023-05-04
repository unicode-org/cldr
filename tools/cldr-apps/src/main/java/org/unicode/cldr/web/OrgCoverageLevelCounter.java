package org.unicode.cldr.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

/** For counting the xpaths in-coverage at a particular level. */
public class OrgCoverageLevelCounter {
    static final Logger logger = SurveyLog.forClass(OrgCoverageLevelCounter.class);

    static final class OrgCoverageLevelCounterHelper {
        static OrgCoverageLevelCounter INSTANCE =
                new OrgCoverageLevelCounter(CookieSession.sm.getSTFactory());
    }

    public static OrgCoverageLevelCounter getInstance() {
        return OrgCoverageLevelCounterHelper.INSTANCE;
    }

    final STFactory factory;
    final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
    final StandardCodes sc = StandardCodes.make();
    final LoadingCache<Pair<Organization, CLDRLocale>, Integer> cache =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<Pair<Organization, CLDRLocale>, Integer>() {
                                @Override
                                public Integer load(Pair<Organization, CLDRLocale> key)
                                        throws Exception {
                                    return handleCountPathsInCoverage(
                                            key.getFirst(), key.getSecond());
                                }
                            });

    public OrgCoverageLevelCounter(STFactory factory) {
        logger.entering("OrgCoverageLevelCounter", "c'tor");
        this.factory = factory;
    }

    public int countPathsInCoverage(final Organization org, final CLDRLocale loc)
            throws ExecutionException {
        return cache.get(Pair.of(org, loc));
    }

    private int handleCountPathsInCoverage(final Organization org, final CLDRLocale loc) {
        logger.entering("OrgCoverageLevelCounter", "countPathsInCoverage", org + ":" + loc);
        Level orgLevel = sc.getLocaleCoverageLevel(org, loc.toString());
        if (orgLevel == null || orgLevel == Level.UNDETERMINED) {
            return 0; // out of coverage = zero paths
        }
        int count = 0;
        final CoverageLevel2 cov = CoverageLevel2.getInstance(sdi, loc.toString());
        final CLDRFile disk = factory.getDiskFile(loc);
        for (final String path : disk.fullIterable()) {
            if (cov.getLevel(path).compareTo(orgLevel) <= 0) {
                count++;
                // could collect paths here
            }
        }
        logger.exiting("OrgCoverageLevelCounter", "countPathsInCoverage");
        return count;
    }
}
