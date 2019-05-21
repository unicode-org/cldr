package org.unicode.cldr.util;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.unicode.cldr.test.CoverageLevel2;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CoverageInfo {
    private final static int MAXLOCALES = 50;

    private final static class XPathWithLocation {
        private final String xpath;
        private final String location;
        private final int hashCode;

        public XPathWithLocation(String xpath, String location) {
            this.xpath = xpath;
            this.location = location;
            this.hashCode = Objects.hash(
                this.xpath,
                this.location);
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (hashCode != other.hashCode()) {
                return false;
            }
            if (!getClass().equals(other.getClass())) {
                return false;
            }
            XPathWithLocation o = (XPathWithLocation) other;
            if (location != null && !location.equals(o.location)) {
                return false;
            }
            if (xpath != null && !xpath.equals(o.xpath)) {
                return false;
            }
            return true;
        }

        public String getXPath() {
            return xpath;
        }

        public String getLocation() {
            return location;
        }

    }

    private Cache<String, CoverageLevel2> localeToCoverageLevelInfo = CacheBuilder.newBuilder().maximumSize(MAXLOCALES).build();
    private Cache<XPathWithLocation, Level> coverageCache = CacheBuilder.newBuilder().maximumSize(MAXLOCALES).build();

    private final SupplementalDataInfo supplementalDataInfo;

    public CoverageInfo(SupplementalDataInfo coverageInfoGettable) {
        this.supplementalDataInfo = coverageInfoGettable;
    }

    /**
     * Used to get the coverage value for a path. This is generally the most
     * efficient way for tools to get coverage.
     *
     * @param xpath
     * @param loc
     * @return
     */
    public Level getCoverageLevel(String xpath, String loc) {
        Level result = null;
        final XPathWithLocation xpLoc = new XPathWithLocation(xpath, loc);
        try {
            result = coverageCache.get(xpLoc, new Callable<Level>() {

                @Override
                public Level call() throws Exception {
                    final String location = xpLoc.getLocation();
                    CoverageLevel2 cov = localeToCoverageLevelInfo.get(location, new Callable<CoverageLevel2>() {

                        @Override
                        public CoverageLevel2 call() throws Exception {
                            return CoverageLevel2.getInstance(supplementalDataInfo, location);
                        }
                    });
                    Level result = cov.getLevel(xpLoc.getXPath());
                    return result;
                }
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Used to get the coverage value for a path. Note, it is more efficient to create
     * a CoverageLevel2 for a language, and keep it around.
     *
     * @param xpath
     * @param loc
     * @return
     */
    public int getCoverageValue(String xpath, String loc) {
        return getCoverageLevel(xpath, loc).getLevel();
    }

}