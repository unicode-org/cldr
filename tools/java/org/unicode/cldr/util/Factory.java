package org.unicode.cldr.util;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile.DraftStatus;

/**
   * A factory is the normal method to produce a set of CLDRFiles from a directory of XML files.
   * See SimpleFactory for a concrete subclass.
   */
  public abstract class Factory {

    private File alternateSupplementalDirectory = null;

    public abstract String getSourceDirectory();

    protected abstract CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus);

    public CLDRFile make(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
      return handleMake(localeID, resolved, madeWithMinimalDraftStatus).setAlternateSupplementalDirectory(alternateSupplementalDirectory);
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

    public File getAlternateSupplementalDirectory() {
      return alternateSupplementalDirectory;
    }

    public Factory setAlternateSupplementalDirectory(File alternateSupplementalDirectory) {
      this.alternateSupplementalDirectory = alternateSupplementalDirectory;
      return this;
    }

  }