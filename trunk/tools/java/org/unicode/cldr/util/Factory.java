package org.unicode.cldr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale.SublocaleProvider;
import org.unicode.cldr.util.XMLSource.ResolvingSource;

/**
 * A factory is the normal method to produce a set of CLDRFiles from a directory of XML files.
 * See SimpleFactory for a concrete subclass.
 */
public abstract class Factory implements SublocaleProvider {

    /**
     * Flag to set more verbose output in makeServolingSource
     */
    private static final boolean DEBUG_FACTORY = false;

    private File supplementalDirectory = null;

    /**
     * Note, the source director(ies) may be a list (seed/common). Therefore, this function is deprecated
     *
     * @deprecated
     * @return the first directory
     */
    public String getSourceDirectory() {
        return getSourceDirectories()[0].getAbsolutePath();
    }

    /**
     * Note, the source director(ies) may be a list (seed/common).
     *
     * @return the first directory
     */
    public abstract File[] getSourceDirectories();

    /**
     * Which source directory does this particular localeID belong to?
     *
     * @param localeID
     * @return
     */
    @Deprecated
    public final File getSourceDirectoryForLocale(String localeID) {
        List<File> temp = getSourceDirectoriesForLocale(localeID);
        return temp == null ? null : temp.get(0);
    }

    /**
     * Classify the tree according to type (maturity)
     *
     * @author srl
     *
     */
    public enum SourceTreeType {
        common, seed, other
    };

    /**
     * Returns the source tree type of either an XML file or its parent directory.
     *
     * @param fileOrDir
     * @return
     */
    public static final SourceTreeType getSourceTreeType(File fileOrDir) {
        if (fileOrDir == null) return null;
        File parentDir = fileOrDir.isFile() ? fileOrDir.getParentFile() : fileOrDir;
        File grandparentDir = parentDir.getParentFile();

        try {
            return SourceTreeType.valueOf(grandparentDir.getName());
        } catch (IllegalArgumentException iae) {
            try {
                return SourceTreeType.valueOf(parentDir.getName());
            } catch (IllegalArgumentException iae2) {
                return SourceTreeType.other;
            }
        }
    }

    public enum DirectoryType {
        main, supplemental, bcp47, casing, collation, dtd, rbnf, segments, transforms, other
    };

    public static final DirectoryType getDirectoryType(File fileOrDir) {
        if (fileOrDir == null) return null;
        File parentDir = fileOrDir.isFile() ? fileOrDir.getParentFile() : fileOrDir;

        try {
            return DirectoryType.valueOf(parentDir.getName());
        } catch (IllegalArgumentException iae2) {
            return DirectoryType.other;
        }
    }

    protected abstract CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus);

    public CLDRFile make(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
        return handleMake(localeID, resolved, madeWithMinimalDraftStatus)
            .setSupplementalDirectory(getSupplementalDirectory());
    }

    public CLDRFile make(String localeID, boolean resolved, boolean includeDraft) {
        return make(localeID, resolved, includeDraft ? DraftStatus.unconfirmed : DraftStatus.approved);
    }

    public CLDRFile make(String localeID, boolean resolved) {
        return make(localeID, resolved, getMinimalDraftStatus());
    }

    public CLDRFile makeWithFallback(String localeID) {
        return makeWithFallback(localeID, getMinimalDraftStatus());
    }

    public CLDRFile makeWithFallback(String localeID, DraftStatus madeWithMinimalDraftStatus) {
        String currentLocaleID = localeID;
        Set<String> availableLocales = this.getAvailable();
        while (!availableLocales.contains(currentLocaleID) && currentLocaleID != "root") {
            currentLocaleID = LocaleIDParser.getParent(currentLocaleID);
        }
        return make(currentLocaleID, true, madeWithMinimalDraftStatus);
    }

    public static XMLSource makeResolvingSource(List<XMLSource> sources) {
        return new ResolvingSource(sources);
    }

    /**
     * Temporary wrapper for creating an XMLSource. This is a hack and should
     * only be used in the Survey Tool for now.
     *
     * @param localeID
     * @return
     */
    public final XMLSource makeSource(String localeID) {
        return make(localeID, false).dataSource;
    }

    /**
     * Creates a resolving source for the given locale ID.
     *
     * @param localeID
     * @param madeWithMinimalDraftStatus
     * @return
     */
    protected ResolvingSource makeResolvingSource(String localeID, DraftStatus madeWithMinimalDraftStatus) {
        List<XMLSource> sourceList = new ArrayList<XMLSource>();
        String curLocale = localeID;
        while (curLocale != null) {
            if (DEBUG_FACTORY) {
                System.out.println("Factory.makeResolvingSource: calling handleMake for locale " +
                    curLocale + " and MimimalDraftStatus " + madeWithMinimalDraftStatus);
            }
            CLDRFile file = handleMake(curLocale, false, madeWithMinimalDraftStatus);
            if (file == null) {
                throw new NullPointerException(this + ".handleMake returned a null CLDRFile for " + curLocale);
            }
            XMLSource source = file.dataSource;
            sourceList.add(source);
            curLocale = LocaleIDParser.getParent(curLocale);
        }
        return new ResolvingSource(sourceList);
    }

    public abstract DraftStatus getMinimalDraftStatus();

    /**
     * Convenience static
     *
     * @param path
     * @param string
     * @return
     */
    public static Factory make(String path, String string) {
        try {
            return SimpleFactory.make(path, string);
        } catch (Exception e) {
            throw new IllegalArgumentException("path: " + path + "; string: " + string, e);
        }
    }

    /**
     * Convenience static
     *
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
     *
     * @param isProper
     *            if false, then parent itself will match
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
     *
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

    /**
     * These factory implementations don't do any caching.
     */
    public Set<CLDRLocale> subLocalesOf(CLDRLocale forLocale) {
        return calculateSubLocalesOf(forLocale, getAvailableCLDRLocales());
    }

    /**
     * Helper function.
     *
     * @return
     */
    public Set<CLDRLocale> getAvailableCLDRLocales() {
        return CLDRLocale.getInstance(getAvailable());
    }

    /**
     * Helper function. Does not cache.
     *
     * @param locale
     * @param available
     * @return
     */
    public Set<CLDRLocale> calculateSubLocalesOf(CLDRLocale locale, Set<CLDRLocale> available) {
        Set<CLDRLocale> sub = new TreeSet<CLDRLocale>();
        for (CLDRLocale l : available) {
            if (l.getParent() == locale) {
                sub.add(l);
            }
        }
        return sub;
    }

    /**
     * Get all of the files in the source directories that match localeName (which is really xml file name).
     * @param localeName
     * @return
     */
    public abstract List<File> getSourceDirectoriesForLocale(String localeName);
}