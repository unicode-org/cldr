package org.unicode.cldr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.SimpleFactory.CLDRCacheKey;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class CLDRFileCache {

    public static final CLDRFileCache SINGLETON = new CLDRFileCache();

    private static final int CACHE_LIMIT = 75;

    Cache<CLDRCacheKey, CLDRFile> cache;

    public static boolean USE_GLOBAL_CLDRFILE_CACHE = CldrUtility.getProperty("USE_GLOBAL_CLDRFILE_CACHE", true);

    public CLDRFileCache () {
        cache =  CacheBuilder.newBuilder().maximumSize(CACHE_LIMIT).build();
    }


    public CLDRFile getCLDRFile (String localeName, List<File> parentDirs, DraftStatus minimalDraftStatus) {
        CLDRFile res;
        CLDRCacheKey cacheKey = new CLDRCacheKey(localeName, false, minimalDraftStatus, parentDirs);
        res = cache.getIfPresent(cacheKey);

        if (res != null) {
            return res;
        }

        synchronized (cache) {
            res = cache.getIfPresent(cacheKey);
            if (res != null) {
                return res;
            }

            List<CLDRFile> list = new ArrayList<>();
            for (File dir: parentDirs) {
                CLDRCacheKey fileCacheKey = new CLDRCacheKey(localeName, false, minimalDraftStatus, Arrays.asList(new File[] {dir}));
                CLDRFile cldrFile = cache.getIfPresent(fileCacheKey);
                if (cldrFile == null) {
                    cldrFile = SimpleFactory.makeFile(localeName, dir, minimalDraftStatus);
                    // check frozen
                    if (!cldrFile.isFrozen()) {
                        cldrFile.freeze();
                    }
                    cache.put(fileCacheKey, cldrFile);
                }
                list.add(cldrFile);
            }

            if (list.size() == 1) {
                return list.get(0);
            } else {
                // merge all CLDRFile into one CLDRFile
                CLDRFile combinedCLDRFile = list.get(0).cloneAsThawed();
                for (int i = 1; i < list.size(); i++) {
                    combinedCLDRFile.putAll(list.get(i), CLDRFile.MERGE_KEEP_MINE);
                }
                // check frozen
                if (!combinedCLDRFile.isFrozen()) {
                    combinedCLDRFile.freeze();
                }
                cache.put(cacheKey, combinedCLDRFile);
                return combinedCLDRFile;
            }
        }
    }



}
