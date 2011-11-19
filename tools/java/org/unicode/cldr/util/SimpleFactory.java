package org.unicode.cldr.util;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.SimpleXMLSource;

public class SimpleFactory extends Factory {
    /**
     * The maximum cache size the caches in SimpleFactory.
     * 15 is a safe limit for instances with limited amounts of memory (around 128MB).
     * Larger numbers are tolerable if more memory is available.
     * This constant may be moved to CldrUtilities in future if needed.
     */
    private static final int CACHE_LIMIT = 15;

    private String sourceDirectory;
    private String matchString;
    private Set<String> localeList = new TreeSet<String>();
    private Map<String,CLDRFile>[] mainCache = new Map[DraftStatus.values().length];
    private Map<String,CLDRFile>[] resolvedCache = new Map[DraftStatus.values().length];
    {
      for (int i = 0; i < mainCache.length; ++i) {
        mainCache[i] = new LruMap<String, CLDRFile>(CACHE_LIMIT);
        resolvedCache[i] = new LruMap<String, CLDRFile>(CACHE_LIMIT);
      }
    }

    private DraftStatus minimalDraftStatus = DraftStatus.unconfirmed;
    private SimpleFactory() {}

    protected DraftStatus getMinimalDraftStatus() {
      return minimalDraftStatus;
    }

    /**
     * Create a factory from a source directory, matchingString, and an optional log file.
     * For the matchString meaning, see {@link getMatchingXMLFiles}
     */
    public static Factory make(String sourceDirectory, String matchString) {
      return make(sourceDirectory, matchString, DraftStatus.unconfirmed);
    }

    public static Factory make(String sourceDirectory, String matchString, DraftStatus minimalDraftStatus) {
      SimpleFactory result = new SimpleFactory();
      result.sourceDirectory = sourceDirectory;
      result.matchString = matchString;
      result.minimalDraftStatus = minimalDraftStatus;
      Matcher m = Pattern.compile(matchString).matcher("");
      result.localeList = CLDRFile.getMatchingXMLFiles(sourceDirectory, m);
      //      try {
      //        result.localeList.addAll(getMatchingXMLFiles(sourceDirectory + "/../supplemental/", m));
      //      } catch(Throwable t) {
      //        throw new Error("CLDRFile unable to load Supplemental data: couldn't getMatchingXMLFiles("+sourceDirectory + "/../supplemental"+")",t);
      //      }
      return result;
    }


    protected Set<String> handleGetAvailable() {
      return localeList;
    }


    private boolean needToReadRoot = true;

    /**
     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
     * use clone().
     */
    // TODO resolve aliases

    public CLDRFile handleMake(String localeName, boolean resolved, DraftStatus minimalDraftStatus) {
      // TODO fix hack: 
      // read root first so that we get the ordering right.
      /*			if (needToReadRoot) {
       if (!localeName.equals("root")) make("root", false);
       needToReadRoot = false;
       }
       */			// end of hack
      Map<String,CLDRFile> cache = resolved ? resolvedCache[minimalDraftStatus.ordinal()] : mainCache[minimalDraftStatus.ordinal()];

      CLDRFile result = cache.get(localeName);
      if (result == null) {
        final String dir = CLDRFile.isSupplementalName(localeName) ? sourceDirectory.replace("incoming/vetted/","common/") + File.separator + "../supplemental/" : sourceDirectory;
        result = CLDRFile.make(localeName, dir, minimalDraftStatus);
        SimpleXMLSource mySource = (SimpleXMLSource)result.dataSource;
        mySource.factory = this;
        mySource.madeWithMinimalDraftStatus = minimalDraftStatus;
        if (resolved) {
          result.dataSource = result.dataSource.getResolving();
        } else {
          result.freeze();	    			
        }
        cache.put(localeName, result);
      }
      return result;
    }

    public String getSourceDirectory() {
      return sourceDirectory;
    }

  }