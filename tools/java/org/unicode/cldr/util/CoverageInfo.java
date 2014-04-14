package org.unicode.cldr.util;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.unicode.cldr.test.CoverageLevel2;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CoverageInfo {
    private final static int MAXLOCALES=50;
    /**
     * Cache Data structure with object expiry,
     * List that can hold up to MAX_LOCALES caches of locales, when one locale hasn't been used for a while it will removed and GC'd
     */

//    private class CoverageCache {
//        private final Deque<Node> localeList=new LinkedList<>();
//        private final int MAX_LOCALES = 10;

        /**
         * Object to sync on for modifying the locale list
         */
//        private final Object LOCALE_LIST_ITER_SYNC=new Object();
        /*
         * constructor
         */
//        public CoverageCache() {
////            localeList = new LinkedList<Node>();
//        }

       
        /*
         * retrieves coverage level associated with two keys if it exists in the cache, otherwise returns null
         * @param xpath
         * @param loc
         * @return the coverage level of the above two keys
         */
//        public Level get(String xpath, String loc) {
//            synchronized(LOCALE_LIST_ITER_SYNC) {
//                Iterator<Node> it=localeList.iterator();
//                Node reAddNode=null;
//                while (it.hasNext())  {
////            for (Iterator<Node> it = localeList.iterator(); it.hasNext();) {
//                    Node node = it.next();
//                    if (node.loc.equals(loc)) {
//                        reAddNode=node;
//                        it.remove();
//                        break;
//
//                    }
//                }
//                if (reAddNode!=null) {
//                    localeList.addFirst(reAddNode);
//                    return reAddNode.map.get(xpath);
//                }
//                return null;
//            }
//        }

        /*
         * places a coverage level into the cache, with two keys
         * @param xpath
         * @param loc
         * @param covLevel    the coverage level of the above two keys
         */
//        public void put(String xpath, String loc, Level covLevel) {
//            synchronized(LOCALE_LIST_ITER_SYNC) {
//                //if locale's map is already in the cache add to it
////            for (Iterator<Node> it = localeList.iterator(); it.hasNext();) {
//                for (Node node: localeList) {
////                Node node = it.next();
//                    if (node.loc.equals(loc)) {
//                        node.map.put(xpath, covLevel);
//                        return;
//                    }
//                }
//
//                //if it is not, add a new map with the coverage level, and remove the last map in the list (used most seldom) if the list is too large
//                Map<String, Level> newMap = new ConcurrentHashMap<String, Level>();
//                newMap.put(xpath, covLevel);
//                localeList.addFirst(new Node(loc, newMap));
//
//                if (localeList.size() > MAX_LOCALES) {
//                    localeList.removeLast();
//                }
//            }
//        }
//
//    }

    private final static class XPathWithLocation{
        private final String xpath;
        private final String location;
        private final int hashCode;
        
        public XPathWithLocation(String xpath, String location) {
            this.xpath=xpath;
            this.location=location;
            this.hashCode=Objects.hash(
                this.xpath,
                this.location);
        }
        
        public int hashCode() {
            return hashCode;
        }
        
        public boolean equals(Object other) {
            if (other==null) {
                return false;
            }
            if (this==other) {
                return true;
            }
            if (hashCode!=other.hashCode()) {
                return false;
            }
            if (!getClass().equals(other.getClass())) {
                return  false;
            }
            XPathWithLocation o=(XPathWithLocation)other;
            if (location!=null && !location.equals(o.location)) {
                return false;
            }
            if (xpath!=null && !xpath.equals(o.xpath)) {
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
    /*
     * node to hold a location and a Map
     */
//    private class Node {
//        //public fields to emulate a C/C++ struct
//        public String loc;
//        public Map<String, Level> map;
//
//        public Node(String _loc, Map<String, Level> _map) {
//            loc = _loc;
//            map = _map;
//        }
//    }
  
    private Cache<String,CoverageLevel2> localeToCoverageLevelInfo=CacheBuilder.newBuilder().maximumSize(MAXLOCALES).build();
//    private Map<String, CoverageLevel2> localeToCoverageLevelInfo = new ConcurrentHashMap<String, CoverageLevel2>();
//    private CoverageCache coverageCache = new CoverageCache();
    private Cache<XPathWithLocation,Level> coverageCache=CacheBuilder.newBuilder().maximumSize(MAXLOCALES).build();
    
    private final CoverageInformationGettable infoGettable;
   
    public CoverageInfo(CoverageInformationGettable coverageInfoGettable) {
        this.infoGettable=coverageInfoGettable;
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
        final XPathWithLocation xpLoc=new XPathWithLocation(xpath, loc);
     //   result = coverageCache.get(xpath, loc);
        try {
            result=coverageCache.get(xpLoc, new Callable<Level>() {

                @Override
                public Level call() throws Exception {
                    final String location=xpLoc.getLocation();
                    CoverageLevel2 cov = localeToCoverageLevelInfo.get(location,new Callable<CoverageLevel2>() {

                        @Override
                        public CoverageLevel2 call() throws Exception {
                           return CoverageLevel2.getInstance(infoGettable, location);
                        }
                    });
//                    if (cov == null) {
//                        cov = CoverageLevel2.getInstance(infoGettable, location);
//                        localeToCoverageLevelInfo.put(location, cov);
//                    }
                    Level result = cov.getLevel(xpLoc.getXPath());
                    return result;
                }
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
//        if (result == null) {
//            CoverageLevel2 cov = localeToCoverageLevelInfo.get(loc);
//            if (cov == null) {
//                cov = CoverageLevel2.getInstance(infoGettable, loc);
//                localeToCoverageLevelInfo.put(loc, cov);
//            }
//
//            result = cov.getLevel(xpath);
//            coverageCache.put(xpath, loc, result);
//        }
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
