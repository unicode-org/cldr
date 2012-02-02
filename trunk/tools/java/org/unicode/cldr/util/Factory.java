package org.unicode.cldr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.XMLSource.ResolvingSource;

/**
   * A factory is the normal method to produce a set of CLDRFiles from a directory of XML files.
   * See SimpleFactory for a concrete subclass.
   */
  public abstract class Factory {

    private File supplementalDirectory = null;

    public abstract String getSourceDirectory();

    protected abstract CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus);

    public CLDRFile make(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
      return handleMake(localeID, resolved, madeWithMinimalDraftStatus).setSupplementalDirectory(supplementalDirectory);
    }

    public CLDRFile make(String localeID, boolean resolved, boolean includeDraft) {
      return make(localeID, resolved, includeDraft ? DraftStatus.unconfirmed : DraftStatus.approved);
    }

    public CLDRFile make(String localeID, boolean resolved) {
      return make(localeID, resolved, getMinimalDraftStatus());
    }

    public CLDRFile makeWithFallback(String localeID) {
      return makeWithFallback(localeID,getMinimalDraftStatus());
    }

    public CLDRFile makeWithFallback(String localeID, DraftStatus madeWithMinimalDraftStatus) {
      String currentLocaleID = localeID;
      Set<String> availableLocales = this.getAvailable();
      while ( !availableLocales.contains(currentLocaleID) && currentLocaleID != "root") {
        currentLocaleID = LocaleIDParser.getParent(currentLocaleID);
      }
      return make(currentLocaleID,true,madeWithMinimalDraftStatus);
    }
    
    public static XMLSource makeResolvingSource(List<XMLSource> sources) {
        return new ResolvingSource(sources);
    }
    
    /**
     * Temporary wrapper for creating an XMLSource. This is a hack and should
     * only be used in the Survey Tool for now.
     * @param localeID
     * @return
     */
    public final XMLSource makeSource(String localeID) {
        return make(localeID, false).dataSource;
    }
    
    /**
     * Creates a resolving source for the given locale ID.
     * @param localeID
     * @param madeWithMinimalDraftStatus
     * @return
     */
    protected ResolvingSource makeResolvingSource(String localeID, DraftStatus madeWithMinimalDraftStatus) {
        List<XMLSource> sourceList = new ArrayList<XMLSource>();
        String curLocale = localeID;
        while(curLocale != null) {
            XMLSource source = handleMake(curLocale, false, madeWithMinimalDraftStatus).dataSource;
            sourceList.add(source);
            curLocale = LocaleIDParser.getParent(curLocale);
        }
        return new ResolvingSource(sourceList);
    }
    
    protected abstract DraftStatus getMinimalDraftStatus();

    /**
     * Convenience static
     * @param path
     * @param string
     * @return
     */
    public static Factory make(String path, String string) {
      try {
        return SimpleFactory.make(path,string);
      } catch (Exception e) {
        throw new IllegalArgumentException("path: " + path + "; string: " + string, e);
      }
    }

    /**
     * Convenience static
     * @param mainDirectory
     * @param string
     * @param approved
     * @return
     */
    public static Factory make(String mainDirectory, String string, DraftStatus approved) {
      return SimpleFactory.make(mainDirectory, string, approved);
    }


    /**
     * Get a set of the available locales for the factory.
     */
    public Set<String> getAvailable() {
      return Collections.unmodifiableSet(handleGetAvailable());
    }
    protected abstract Set<String> handleGetAvailable();

    /**
     * Get a set of the available language locales (according to isLanguage).
     */
    public Set<String> getAvailableLanguages() {
      Set<String> result = new TreeSet<String>();
      for (Iterator<String> it = handleGetAvailable().iterator(); it.hasNext();) {
        String s = it.next();
        if (XPathParts.isLanguage(s)) result.add(s);
      }
      return result;
    }

    /**
     * Get a set of the locales that have the given parent (according to isSubLocale())
     * @param isProper if false, then parent itself will match
     */
    public Set<String> getAvailableWithParent(String parent, boolean isProper) {
      Set<String> result = new TreeSet<String>();

      for (Iterator<String> it = handleGetAvailable().iterator(); it.hasNext();) {
        String s = it.next();
        int relation = XPathParts.isSubLocale(parent, s);
        if (relation >= 0 && !(isProper && relation == 0)) result.add(s);
      }
      return result;
    }

    public File getSupplementalDirectory() {
      return supplementalDirectory;
    }

    /**
     * Sets the supplemental directory to be used by this Factory and CLDRFiles
     * created by this Factory.
     * @param supplementalDirectory
     * @return
     */
    public Factory setSupplementalDirectory(File supplementalDirectory) {
      this.supplementalDirectory = supplementalDirectory;
      return this;
    }

    // TODO(jchye): Clean this up.
    public CLDRFile getSupplementalData() {
      try {
        return make("supplementalData", false);
      } catch (RuntimeException e) {
        return Factory.make(getSupplementalDirectory().getPath(), ".*").make("supplementalData", false);
      }
    }
    
    public CLDRFile getSupplementalMetadata() {
      try {
        return make("supplementalMetadata", false);
      } catch (RuntimeException e) {
        return Factory.make(getSupplementalDirectory().getPath(), ".*").make("supplementalMetadata", false);
      }
    }
  }