/*
 ******************************************************************************
 * Copyright (C) 2005-2011, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.util;

import com.google.common.collect.Iterators;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.LocaleInheritanceInfo.Reason;
import org.unicode.cldr.util.XPathParts.Comments;
import org.xml.sax.Locator;

/**
 * Overall process is described in
 * http://cldr.unicode.org/development/development-process/design-proposals/resolution-of-cldr-files
 * Please update that document if major changes are made.
 */
public abstract class XMLSource implements Freezable<XMLSource>, Iterable<String> {
    public static final String CODE_FALLBACK_ID = "code-fallback";
    public static final String ROOT_ID = "root";
    public static final boolean USE_PARTS_IN_ALIAS = false;
    private static final String TRACE_INDENT = " "; // "\t"
    private static Map<String, String> allowDuplicates = new HashMap<>();

    private String localeID;
    private boolean nonInheriting;
    private TreeMap<String, String> aliasCache;
    private LinkedHashMap<String, List<String>> reverseAliasCache;
    protected boolean locked;
    transient String[] fixedPath = new String[1];

    /**
     * This class represents a source location of an XPath.
     *
     * @see com.ibm.icu.dev.test.TestFmwk.SourceLocation
     */
    public static class SourceLocation {
        static final String FILE_PREFIX = "file://";
        private String system;
        private int line;
        private int column;

        /**
         * Initialize from an XML Locator
         *
         * @param locator
         */
        public SourceLocation(Locator locator) {
            this(locator.getSystemId(), locator.getLineNumber(), locator.getColumnNumber());
        }

        public SourceLocation(String system, int line, int column) {
            this.system = system.intern();
            this.line = line;
            this.column = column;
        }

        public String getSystem() {
            // Trim prefix lazily.
            if (system.startsWith(FILE_PREFIX)) {
                return system.substring(FILE_PREFIX.length());
            } else {
                return system;
            }
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        /**
         * The toString() format is suitable for printing to the command line and has the format
         * 'file:line:column: '
         */
        @Override
        public String toString() {
            return toString(null);
        }

        /**
         * The toString() format is suitable for printing to the command line and has the format
         * 'file:line:column: ' A good leading base path might be CLDRPaths.BASE_DIRECTORY
         *
         * @param basePath path to trim
         */
        public String toString(final String basePath) {
            return getSystem(basePath) + ":" + getLine() + ":" + getColumn() + ": ";
        }

        /**
         * Format location suitable for GitHub annotations, skips leading base bath A good leading
         * base path might be CLDRPaths.BASE_DIRECTORY
         *
         * @param basePath path to trim
         * @return
         */
        public String forGitHub(String basePath) {
            return "file=" + getSystem(basePath) + ",line=" + getLine() + ",col=" + getColumn();
        }

        /** Format location suitable for GitHub annotations */
        public String forGitHub() {
            return forGitHub(null);
        }

        /**
         * as with getSystem(), but skips the leading base path if identical. A good leading path
         * might be CLDRPaths.BASE_DIRECTORY
         *
         * @param basePath path to trim
         */
        public String getSystem(String basePath) {
            String path = getSystem();
            if (basePath != null && !basePath.isEmpty() && path.startsWith(basePath)) {
                path = path.substring(basePath.length());
                // Handle case where the path did NOT start with a slash
                if (path.startsWith("/") && !basePath.endsWith("/")) {
                    path = path.substring(1); // skip leading /
                }
            }
            return path;
        }
    }

    /*
     * For testing, make it possible to disable multiple caches:
     * getFullPathAtDPathCache, getSourceLocaleIDCache, aliasCache, reverseAliasCache
     */
    protected boolean cachingIsEnabled = true;

    public void disableCaching() {
        cachingIsEnabled = false;
    }

    public static class AliasLocation {
        public final String pathWhereFound;
        public final String localeWhereFound;

        public AliasLocation(String pathWhereFound, String localeWhereFound) {
            this.pathWhereFound = pathWhereFound;
            this.localeWhereFound = localeWhereFound;
        }
    }

    // Listeners are stored using weak references so that they can be garbage collected.
    private List<WeakReference<Listener>> listeners = new ArrayList<>();

    public String getLocaleID() {
        return localeID;
    }

    public void setLocaleID(String localeID) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        this.localeID = localeID;
    }

    /**
     * Adds all the path,value pairs in tempMap. The paths must be Full Paths.
     *
     * @param tempMap
     * @param conflict_resolution
     */
    public void putAll(Map<String, String> tempMap, int conflict_resolution) {
        for (Iterator<String> it = tempMap.keySet().iterator(); it.hasNext(); ) {
            String path = it.next();
            if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValueAtPath(path) != null)
                continue;
            putValueAtPath(path, tempMap.get(path));
        }
    }

    /**
     * Adds all the path, value pairs in otherSource.
     *
     * @param otherSource
     * @param conflict_resolution
     */
    public void putAll(XMLSource otherSource, int conflict_resolution) {
        for (Iterator<String> it = otherSource.iterator(); it.hasNext(); ) {
            String path = it.next();
            final String oldValue = getValueAtDPath(path);
            if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && oldValue != null) {
                continue;
            }
            final String newValue = otherSource.getValueAtDPath(path);
            if (newValue.equals(oldValue)) {
                continue;
            }
            String fullPath = putValueAtPath(otherSource.getFullPathAtDPath(path), newValue);
            addSourceLocation(fullPath, otherSource.getSourceLocation(fullPath));
        }
    }

    /**
     * Removes all the paths in the collection. WARNING: must be distinguishedPaths
     *
     * @param xpaths
     */
    public void removeAll(Collection<String> xpaths) {
        for (Iterator<String> it = xpaths.iterator(); it.hasNext(); ) {
            removeValueAtDPath(it.next());
        }
    }

    /**
     * Tests whether the full path for this dpath is draft or now.
     *
     * @param path
     * @return
     */
    public boolean isDraft(String path) {
        String fullpath = getFullPath(path);
        if (path == null) {
            return false;
        }
        if (fullpath.indexOf("[@draft=") < 0) {
            return false;
        }
        XPathParts parts = XPathParts.getFrozenInstance(fullpath);
        return parts.containsAttribute("draft");
    }

    @Override
    public boolean isFrozen() {
        return locked;
    }

    /**
     * Adds the path,value pair. The path must be full path.
     *
     * @param xpath
     * @param value
     */
    public String putValueAtPath(String xpath, String value) {
        xpath = xpath.intern();
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath);
        putValueAtDPath(distinguishingXPath, value);
        if (!fixedPath[0].equals(distinguishingXPath)) {
            clearCache();
            putFullPathAtDPath(distinguishingXPath, fixedPath[0]);
        }
        return distinguishingXPath;
    }

    /** Gets those paths that allow duplicates */
    public static Map<String, String> getPathsAllowingDuplicates() {
        return allowDuplicates;
    }

    /** A listener for XML source data. */
    public static interface Listener {
        /**
         * Called whenever the source being listened to has a data change.
         *
         * @param xpath The xpath that had its value changed.
         * @param source back-pointer to the source that changed
         */
        public void valueChanged(String xpath, XMLSource source);
    }

    /** Internal class. Immutable! */
    public static final class Alias {
        private final String newLocaleID;
        private final String oldPath;
        private final String newPath;
        private final boolean pathsEqual;
        static final Pattern aliasPattern =
                Pattern.compile(
                        "(?:\\[@source=\"([^\"]*)\"])?(?:\\[@path=\"([^\"]*)\"])?(?:\\[@draft=\"([^\"]*)\"])?");
        // constant, so no need to sync

        public static Alias make(String aliasPath) {
            int pos = aliasPath.indexOf("/alias");
            if (pos < 0) return null; // quickcheck
            String aliasParts = aliasPath.substring(pos + 6);
            String oldPath = aliasPath.substring(0, pos);
            String newPath = null;

            return new Alias(pos, oldPath, newPath, aliasParts);
        }

        private Alias(int pos, String oldPath, String newPath, String aliasParts) {
            Matcher matcher = aliasPattern.matcher(aliasParts);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("bad alias pattern for " + aliasParts);
            }
            String newLocaleID = matcher.group(1);
            if (newLocaleID != null && newLocaleID.equals("locale")) {
                newLocaleID = null;
            }
            String relativePath2 = matcher.group(2);
            if (newPath == null) {
                newPath = oldPath;
            }
            if (relativePath2 != null) {
                newPath = addRelative(newPath, relativePath2);
            }

            boolean pathsEqual = oldPath.equals(newPath);

            if (pathsEqual && newLocaleID == null) {
                throw new IllegalArgumentException(
                        "Alias must have different path or different source. AliasPath: "
                                + aliasParts
                                + ", Alias: "
                                + newPath
                                + ", "
                                + newLocaleID);
            }

            this.newLocaleID = newLocaleID;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.pathsEqual = pathsEqual;
        }

        /**
         * Create a new path from an old path + relative portion. Basically, each ../ at the front
         * of the relative portion removes a trailing element+attributes from the old path.
         * WARNINGS: 1. It could fail if an attribute value contains '/'. This should not be the
         * case except in alias elements, but need to verify. 2. Also assumes that there are no
         * extra /'s in the relative or old path. 3. If we verified that the relative paths always
         * used " in place of ', we could also save a step.
         *
         * <p>Maybe we could clean up #2 and #3 when reading in a CLDRFile the first time?
         *
         * @param oldPath
         * @param relativePath
         * @return
         */
        static String addRelative(String oldPath, String relativePath) {
            if (relativePath.startsWith("//")) {
                return relativePath;
            }
            while (relativePath.startsWith("../")) {
                relativePath = relativePath.substring(3);
                // strip extra "/". Shouldn't occur, but just to be safe.
                while (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                // strip last element
                oldPath = stripLastElement(oldPath);
            }
            return oldPath + "/" + relativePath.replace('\'', '"');
        }

        static final Pattern MIDDLE_OF_ATTRIBUTE_VALUE = PatternCache.get("[^\"]*\"\\]");

        public static String stripLastElement(String oldPath) {
            int oldPos = oldPath.lastIndexOf('/');
            // verify that we are not in the middle of an attribute value
            Matcher verifyElement = MIDDLE_OF_ATTRIBUTE_VALUE.matcher(oldPath.substring(oldPos));
            while (verifyElement.lookingAt()) {
                oldPos = oldPath.lastIndexOf('/', oldPos - 1);
                // will throw exception if we didn't find anything
                verifyElement.reset(oldPath.substring(oldPos));
            }
            oldPath = oldPath.substring(0, oldPos);
            return oldPath;
        }

        @Override
        public String toString() {
            return "newLocaleID: "
                    + newLocaleID
                    + ",\t"
                    + "oldPath: "
                    + oldPath
                    + ",\n\t"
                    + "newPath: "
                    + newPath;
        }

        /**
         * This function is called on the full path, when we know the distinguishing path matches
         * the oldPath. So we just want to modify the base of the path
         */
        public String changeNewToOld(String fullPath, String newPath, String oldPath) {
            // do common case quickly
            if (fullPath.startsWith(newPath)) {
                return oldPath + fullPath.substring(newPath.length());
            }

            // fullPath will be the same as newPath, except for some attributes at the end.
            // add those attributes to oldPath, starting from the end.
            XPathParts partsOld = XPathParts.getFrozenInstance(oldPath);
            XPathParts partsNew = XPathParts.getFrozenInstance(newPath);
            XPathParts partsFull = XPathParts.getFrozenInstance(fullPath);
            Map<String, String> attributesFull = partsFull.getAttributes(-1);
            Map<String, String> attributesNew = partsNew.getAttributes(-1);
            Map<String, String> attributesOld = partsOld.getAttributes(-1);
            for (Iterator<String> it = attributesFull.keySet().iterator(); it.hasNext(); ) {
                String attribute = it.next();
                if (attributesNew.containsKey(attribute)) continue;
                attributesOld.put(attribute, attributesFull.get(attribute));
            }
            String result = partsOld.toString();
            return result;
        }

        public String getOldPath() {
            return oldPath;
        }

        public String getNewLocaleID() {
            return newLocaleID;
        }

        public String getNewPath() {
            return newPath;
        }

        public String composeNewAndOldPath(String path) {
            return newPath + path.substring(oldPath.length());
        }

        public String composeOldAndNewPath(String path) {
            return oldPath + path.substring(newPath.length());
        }

        public boolean pathsEqual() {
            return pathsEqual;
        }

        public static boolean isAliasPath(String path) {
            return path.contains("/alias");
        }
    }

    /**
     * This method should be overridden.
     *
     * @return a mapping of paths to their aliases. Note that since root is the only locale to have
     *     aliases, all other locales will have no mappings.
     */
    protected synchronized TreeMap<String, String> getAliases() {
        if (!cachingIsEnabled) {
            /*
             * Always create and return a new "aliasMap" instead of this.aliasCache
             * Probably expensive!
             */
            return loadAliases();
        }

        /*
         * The cache assumes that aliases will never change over the lifetime of an XMLSource.
         */
        if (aliasCache == null) {
            aliasCache = loadAliases();
        }
        return aliasCache;
    }

    /**
     * Look for aliases and create mappings for them. Aliases are only ever found in root.
     *
     * <p>return aliasMap the new map
     */
    private TreeMap<String, String> loadAliases() {
        TreeMap<String, String> aliasMap = new TreeMap<>();
        for (String path : this) {
            if (!Alias.isAliasPath(path)) {
                continue;
            }
            path = path.intern();
            String fullPath = getFullPathAtDPath(path).intern();
            Alias temp = Alias.make(fullPath);
            if (temp == null) {
                continue;
            }
            aliasMap.put(temp.getOldPath(), temp.getNewPath());
        }
        return aliasMap;
    }

    /**
     * @return a reverse mapping of aliases
     */
    private LinkedHashMap<String, List<String>> getReverseAliases() {
        if (cachingIsEnabled && reverseAliasCache != null) {
            return reverseAliasCache;
        }
        // Aliases are only ever found in root.
        Map<String, String> aliases = getAliases();
        Map<String, List<String>> reverse = new HashMap<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            List<String> list = reverse.get(entry.getValue());
            if (list == null) {
                list = new ArrayList<>();
                reverse.put(entry.getValue(), list);
            }
            list.add(entry.getKey());
        }
        // Sort map.
        LinkedHashMap<String, List<String>> reverseAliasMap =
                new LinkedHashMap<>(new TreeMap<>(reverse));
        if (cachingIsEnabled) {
            reverseAliasCache = reverseAliasMap;
        }
        return reverseAliasMap;
    }

    /**
     * Clear "any internal caches" (or only aliasCache?) for this XMLSource.
     *
     * <p>Called only by XMLSource.putValueAtPath and XMLSource.removeValueAtPath
     *
     * <p>Note: this method does not affect other caches: reverseAliasCache,
     * getFullPathAtDPathCache, getSourceLocaleIDCache
     */
    private void clearCache() {
        aliasCache = null;
    }

    /**
     * Return the localeID of the XMLSource where the path was found SUBCLASSING: must be overridden
     * in a resolving locale
     *
     * @param path the given path
     * @param status if not null, to have status.pathWhereFound filled in
     * @return the localeID
     */
    public String getSourceLocaleID(String path, CLDRFile.Status status) {
        if (status != null) {
            status.pathWhereFound = CLDRFile.getDistinguishingXPath(path, null);
        }
        return getLocaleID();
    }

    /**
     * Same as getSourceLocaleID, with unused parameter skipInheritanceMarker. This is defined so
     * that the version for ResolvingSource can be defined and called for a ResolvingSource that is
     * declared as an XMLSource.
     *
     * @param path the given path
     * @param status if not null, to have status.pathWhereFound filled in
     * @param skipInheritanceMarker ignored
     * @return the localeID
     */
    public String getSourceLocaleIdExtended(
            String path,
            CLDRFile.Status status,
            @SuppressWarnings("unused") boolean skipInheritanceMarker,
            List<LocaleInheritanceInfo> list) {
        final String locale = getSourceLocaleID(path, status);
        if (list != null) {
            if (hasValueAtDPath(path)) {
                list.add(
                        new LocaleInheritanceInfo(
                                locale, path, LocaleInheritanceInfo.Reason.value));
                // Since we’re not resolving, there’s no way to look for a Bailey value here.
            } else {
                list.add(
                        new LocaleInheritanceInfo(
                                locale, path, LocaleInheritanceInfo.Reason.none)); // not found
            }
        }
        return locale;
    }

    /**
     * Remove the value. SUBCLASSING: must be overridden in a resolving locale
     *
     * @param xpath
     */
    public void removeValueAtPath(String xpath) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        clearCache();
        removeValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
    }

    /**
     * Get the value. SUBCLASSING: must be overridden in a resolving locale
     *
     * @param xpath
     * @return
     */
    public String getValueAtPath(String xpath) {
        return getValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
    }

    /**
     * Get the full path for a distinguishing path SUBCLASSING: must be overridden in a resolving
     * locale
     *
     * @param xpath
     * @return
     */
    public String getFullPath(String xpath) {
        return getFullPathAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
    }

    /**
     * Put the full path for this distinguishing path The caller will have processed the path, and
     * only call this with the distinguishing path SUBCLASSING: must be overridden
     */
    public abstract void putFullPathAtDPath(String distinguishingXPath, String fullxpath);

    /**
     * Put the distinguishing path, value. The caller will have processed the path, and only call
     * this with the distinguishing path SUBCLASSING: must be overridden
     */
    public abstract void putValueAtDPath(String distinguishingXPath, String value);

    /**
     * Remove the path, and the full path, and value corresponding to the path. The caller will have
     * processed the path, and only call this with the distinguishing path SUBCLASSING: must be
     * overridden
     */
    public abstract void removeValueAtDPath(String distinguishingXPath);

    /**
     * Get the value at the given distinguishing path The caller will have processed the path, and
     * only call this with the distinguishing path SUBCLASSING: must be overridden
     */
    public abstract String getValueAtDPath(String path);

    public boolean hasValueAtDPath(String path) {
        return (getValueAtDPath(path) != null);
    }

    /**
     * Get the Last-Change Date (if known) when the value was changed. SUBCLASSING: may be
     * overridden. defaults to NULL.
     *
     * @return last change date (if known), else null
     */
    public Date getChangeDateAtDPath(String path) {
        return null;
    }

    /**
     * Get the full path at the given distinguishing path The caller will have processed the path,
     * and only call this with the distinguishing path SUBCLASSING: must be overridden
     */
    public abstract String getFullPathAtDPath(String path);

    /**
     * Get the comments for the source. TODO: integrate the Comments class directly into this class
     * SUBCLASSING: must be overridden
     */
    public abstract Comments getXpathComments();

    /**
     * Set the comments for the source. TODO: integrate the Comments class directly into this class
     * SUBCLASSING: must be overridden
     */
    public abstract void setXpathComments(Comments comments);

    /**
     * @return an iterator over the distinguished paths
     */
    @Override
    public abstract Iterator<String> iterator();

    /**
     * @return an iterator over the distinguished paths that start with the prefix. SUBCLASSING:
     *     Normally overridden for efficiency
     */
    public Iterator<String> iterator(String prefix) {
        if (prefix == null || prefix.length() == 0) return iterator();
        return Iterators.filter(iterator(), s -> s.startsWith(prefix));
    }

    public Iterator<String> iterator(Matcher pathFilter) {
        if (pathFilter == null) return iterator();
        return Iterators.filter(iterator(), s -> pathFilter.reset(s).matches());
    }

    /**
     * @return returns whether resolving or not SUBCLASSING: Only changed for resolving subclasses
     */
    public boolean isResolving() {
        return false;
    }

    /**
     * Returns the unresolved version of this XMLSource. SUBCLASSING: Override in resolving sources.
     */
    public XMLSource getUnresolving() {
        return this;
    }

    /** SUBCLASSING: must be overridden */
    @Override
    public XMLSource cloneAsThawed() {
        try {
            XMLSource result = (XMLSource) super.clone();
            result.locked = false;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("should never happen");
        }
    }

    /** for debugging only */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = iterator(); it.hasNext(); ) {
            String path = it.next();
            String value = getValueAtDPath(path);
            String fullpath = getFullPathAtDPath(path);
            result.append(fullpath)
                    .append(" =\t ")
                    .append(value)
                    .append(CldrUtility.LINE_SEPARATOR);
        }
        return result.toString();
    }

    /** for debugging only */
    public String toString(String regex) {
        Matcher matcher = PatternCache.get(regex).matcher("");
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = iterator(matcher); it.hasNext(); ) {
            String path = it.next();
            String value = getValueAtDPath(path);
            String fullpath = getFullPathAtDPath(path);
            result.append(fullpath)
                    .append(" =\t ")
                    .append(value)
                    .append(CldrUtility.LINE_SEPARATOR);
        }
        return result.toString();
    }

    /**
     * @return returns whether supplemental or not
     */
    public boolean isNonInheriting() {
        return nonInheriting;
    }

    /**
     * @return sets whether supplemental. Normally only called internall.
     */
    public void setNonInheriting(boolean nonInheriting) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        this.nonInheriting = nonInheriting;
    }

    /**
     * Internal class for doing resolution
     *
     * @author davis
     */
    public static class ResolvingSource extends XMLSource implements Listener {
        private XMLSource currentSource;
        private LinkedHashMap<String, XMLSource> sources;

        @Override
        public boolean isResolving() {
            return true;
        }

        @Override
        public XMLSource getUnresolving() {
            return sources.get(getLocaleID());
        }

        /*
         * If there is an alias, then inheritance gets tricky.
         * If there is a path //ldml/xyz/.../uvw/alias[@path=...][@source=...]
         * then the parent for //ldml/xyz/.../uvw/abc/.../def/
         * is source, and the path to search for is really: //ldml/xyz/.../uvw/path/abc/.../def/
         */
        public static final boolean TRACE_VALUE = CldrUtility.getProperty("TRACE_VALUE", false);

        // Map<String,String> getValueAtDPathCache = new HashMap();

        @Override
        public String getValueAtDPath(String xpath) {
            if (DEBUG_PATH != null && DEBUG_PATH.matcher(xpath).find()) {
                System.out.println("Getting value for Path: " + xpath);
            }
            if (TRACE_VALUE)
                System.out.println(
                        "\t*xpath: "
                                + xpath
                                + CldrUtility.LINE_SEPARATOR
                                + "\t*source: "
                                + currentSource.getClass().getName()
                                + CldrUtility.LINE_SEPARATOR
                                + "\t*locale: "
                                + currentSource.getLocaleID());
            String result = null;
            AliasLocation fullStatus =
                    getCachedFullStatus(xpath, true /* skipInheritanceMarker */, null);
            if (fullStatus != null) {
                if (TRACE_VALUE) {
                    System.out.println("\t*pathWhereFound: " + fullStatus.pathWhereFound);
                    System.out.println("\t*localeWhereFound: " + fullStatus.localeWhereFound);
                }
                result = getSource(fullStatus).getValueAtDPath(fullStatus.pathWhereFound);
            }
            if (TRACE_VALUE) System.out.println("\t*value: " + result);
            return result;
        }

        @Override
        public SourceLocation getSourceLocation(String xpath) {
            SourceLocation result = null;
            final String dPath = CLDRFile.getDistinguishingXPath(xpath, null);
            // getCachedFullStatus wants a dPath
            AliasLocation fullStatus =
                    getCachedFullStatus(dPath, true /* skipInheritanceMarker */, null);
            if (fullStatus != null) {
                result =
                        getSource(fullStatus)
                                .getSourceLocation(xpath); // getSourceLocation wants fullpath
            }
            return result;
        }

        public XMLSource getSource(AliasLocation fullStatus) {
            XMLSource source = sources.get(fullStatus.localeWhereFound);
            return source == null ? constructedItems : source;
        }

        Map<String, String> getFullPathAtDPathCache = new HashMap<>();

        @Override
        public String getFullPathAtDPath(String xpath) {
            String result = currentSource.getFullPathAtDPath(xpath);
            if (result != null) {
                return result;
            }
            // This is tricky. We need to find the alias location's path and full path.
            // then we need to the the non-distinguishing elements from them,
            // and add them into the requested path.
            AliasLocation fullStatus =
                    getCachedFullStatus(xpath, true /* skipInheritanceMarker */, null);
            if (fullStatus != null) {
                String fullPathWhereFound =
                        getSource(fullStatus).getFullPathAtDPath(fullStatus.pathWhereFound);
                if (fullPathWhereFound == null) {
                    result = null;
                } else if (fullPathWhereFound.equals(fullStatus.pathWhereFound)) {
                    result = xpath; // no difference
                } else {
                    result = getFullPath(xpath, fullStatus, fullPathWhereFound);
                }
            }
            return result;
        }

        @Override
        public Date getChangeDateAtDPath(String xpath) {
            Date result = currentSource.getChangeDateAtDPath(xpath);
            if (result != null) {
                return result;
            }
            AliasLocation fullStatus =
                    getCachedFullStatus(xpath, true /* skipInheritanceMarker */, null);
            if (fullStatus != null) {
                result = getSource(fullStatus).getChangeDateAtDPath(fullStatus.pathWhereFound);
            }
            return result;
        }

        private String getFullPath(
                String xpath, AliasLocation fullStatus, String fullPathWhereFound) {
            String result = null;
            xpath = xpath.intern();
            if (this.cachingIsEnabled) {
                result = getFullPathAtDPathCache.get(xpath);
            }
            if (result == null) {
                // find the differences, and add them into xpath
                // we do this by walking through each element, adding the corresponding attribute
                // values.
                // we add attributes FROM THE END, in case the lengths are different!
                XPathParts xpathParts =
                        XPathParts.getFrozenInstance(xpath)
                                .cloneAsThawed(); // not frozen, for putAttributeValue
                XPathParts fullPathWhereFoundParts =
                        XPathParts.getFrozenInstance(fullPathWhereFound);
                XPathParts pathWhereFoundParts =
                        XPathParts.getFrozenInstance(fullStatus.pathWhereFound);
                int offset = xpathParts.size() - pathWhereFoundParts.size();

                for (int i = 0; i < pathWhereFoundParts.size(); ++i) {
                    Map<String, String> fullAttributes = fullPathWhereFoundParts.getAttributes(i);
                    Map<String, String> attributes = pathWhereFoundParts.getAttributes(i);
                    if (!attributes.equals(fullAttributes)) { // add differences
                        for (String key : fullAttributes.keySet()) {
                            if (!attributes.containsKey(key)) {
                                String value = fullAttributes.get(key);
                                xpathParts.putAttributeValue(i + offset, key, value);
                            }
                        }
                    }
                }
                result = xpathParts.toString();
                if (cachingIsEnabled) {
                    getFullPathAtDPathCache.put(xpath, result);
                }
            }
            return result;
        }

        /**
         * Return the "George Bailey" value, i.e., the value that would obtain if the value didn't
         * exist (in the first source). Often the Bailey value comes from the parent locale (such as
         * "fr") of a sublocale (such as "fr_CA"). Sometimes the Bailey value comes from an alias
         * which may be a different path in the same locale.
         *
         * @param xpath the given path
         * @param pathWhereFound if not null, to be filled in with the path where found
         * @param localeWhereFound if not null, to be filled in with the locale where found
         * @return the Bailey value
         */
        @Override
        public String getBaileyValue(
                String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
            AliasLocation fullStatus =
                    getPathLocation(
                            xpath, true /* skipFirst */, true /* skipInheritanceMarker */, null);
            if (localeWhereFound != null) {
                localeWhereFound.value = fullStatus.localeWhereFound;
            }
            if (pathWhereFound != null) {
                pathWhereFound.value = fullStatus.pathWhereFound;
            }
            return getSource(fullStatus).getValueAtDPath(fullStatus.pathWhereFound);
        }

        /**
         * Get the AliasLocation that would be returned by getPathLocation (with skipFirst false),
         * using a cache for efficiency
         *
         * @param xpath the given path
         * @param skipInheritanceMarker if true, skip sources in which value is INHERITANCE_MARKER
         * @return the AliasLocation
         */
        private AliasLocation getCachedFullStatus(
                String xpath, boolean skipInheritanceMarker, List<LocaleInheritanceInfo> list) {
            /*
             * Skip the cache in the special and relatively rare cases where skipInheritanceMarker is false.
             *
             * Note: we might consider using a cache also when skipInheritanceMarker is false.
             * Can't use the same cache for skipInheritanceMarker true and false.
             * Could use two caches, or add skipInheritanceMarker to the key (append 'T' or 'F' to xpath).
             * The situation is complicated by use of getSourceLocaleIDCache also in valueChanged.
             *
             * There is no caching problem with skipFirst, since that is always false here -- though
             * getBaileyValue could use a cache if there was one for skipFirst true.
             *
             * Also skip caching if the list is non-null, for tracing.
             */
            if (!skipInheritanceMarker || !cachingIsEnabled || (list != null)) {
                return getPathLocation(xpath, false /* skipFirst */, skipInheritanceMarker, list);
            }
            synchronized (getSourceLocaleIDCache) {
                AliasLocation fullStatus = getSourceLocaleIDCache.get(xpath);
                if (fullStatus == null) {
                    fullStatus =
                            getPathLocation(
                                    xpath, false /* skipFirst */, skipInheritanceMarker, null);
                    getSourceLocaleIDCache.put(xpath, fullStatus); // cache copy
                }
                return fullStatus;
            }
        }

        @Override
        public String getWinningPath(String xpath) {
            String result = currentSource.getWinningPath(xpath);
            if (result != null) return result;
            AliasLocation fullStatus =
                    getCachedFullStatus(xpath, true /* skipInheritanceMarker */, null);
            if (fullStatus != null) {
                result = getSource(fullStatus).getWinningPath(fullStatus.pathWhereFound);
            } else {
                result = xpath;
            }
            return result;
        }

        private transient Map<String, AliasLocation> getSourceLocaleIDCache = new WeakHashMap<>();

        /**
         * Get the source locale ID for the given path, for this ResolvingSource.
         *
         * @param distinguishedXPath the given path
         * @param status if not null, to have status.pathWhereFound filled in
         * @return the localeID, as a string
         */
        @Override
        public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
            return getSourceLocaleIdExtended(
                    distinguishedXPath, status, true /* skipInheritanceMarker */, null);
        }

        /**
         * Same as ResolvingSource.getSourceLocaleID, with additional parameter
         * skipInheritanceMarker, which is passed on to getCachedFullStatus and getPathLocation.
         *
         * @param distinguishedXPath the given path
         * @param status if not null, to have status.pathWhereFound filled in
         * @param skipInheritanceMarker if true, skip sources in which value is INHERITANCE_MARKER
         * @return the localeID, as a string
         */
        @Override
        public String getSourceLocaleIdExtended(
                String distinguishedXPath,
                CLDRFile.Status status,
                boolean skipInheritanceMarker,
                List<LocaleInheritanceInfo> list) {
            AliasLocation fullStatus =
                    getCachedFullStatus(distinguishedXPath, skipInheritanceMarker, list);
            if (status != null) {
                status.pathWhereFound = fullStatus.pathWhereFound;
            }
            return fullStatus.localeWhereFound;
        }

        static final Pattern COUNT_EQUALS = PatternCache.get("\\[@count=\"[^\"]*\"]");

        /**
         * Get the AliasLocation, containing path and locale where found, for the given path, for
         * this ResolvingSource.
         *
         * @param xpath the given path
         * @param skipFirst true if we're getting the Bailey value (caller is getBaileyValue), else
         *     false (caller is getCachedFullStatus)
         * @param skipInheritanceMarker if true, skip sources in which value is INHERITANCE_MARKER
         * @return the AliasLocation
         *     <p>skipInheritanceMarker must be true when the caller is getBaileyValue, so that the
         *     caller will not return INHERITANCE_MARKER as the George Bailey value. When the caller
         *     is getMissingStatus, we're not getting the Bailey value, and skipping
         *     INHERITANCE_MARKER here could take us up to "root", which getMissingStatus would
         *     misinterpret to mean the item should be listed under Missing in the Dashboard.
         *     Therefore skipInheritanceMarker needs to be false when getMissingStatus is the
         *     caller. Note that we get INHERITANCE_MARKER when there are votes for inheritance, but
         *     when there are no votes getValueAtDPath returns null so we don't get
         *     INHERITANCE_MARKER.
         *     <p>Situation for CheckCoverage.handleCheck may be similar to getMissingStatus, see
         *     ticket 11720.
         *     <p>For other callers, we stick with skipInheritanceMarker true for now, to retain the
         *     behavior before the skipInheritanceMarker parameter was added, but we should be alert
         *     for the possibility that skipInheritanceMarker should be false in some other cases
         *     <p>References: https://unicode.org/cldr/trac/ticket/11765
         *     https://unicode.org/cldr/trac/ticket/11720 https://unicode.org/cldr/trac/ticket/11103
         */
        private AliasLocation getPathLocation(
                String xpath,
                boolean skipFirst,
                boolean skipInheritanceMarker,
                List<LocaleInheritanceInfo> list) {
            xpath = xpath.intern();

            //   When calculating the Bailey values, we track the final
            //   return value as firstValue. If non-null, this will become
            //   the function's ultimate return value.
            AliasLocation firstValue = null;
            for (XMLSource source : sources.values()) {
                if (skipFirst) {
                    skipFirst = false;
                    continue;
                }
                String value = source.getValueAtDPath(xpath);
                String localeID = source.getLocaleID();
                if (value != null) {
                    if (skipInheritanceMarker && CldrUtility.INHERITANCE_MARKER.equals(value)) {
                        if (list != null) {
                            list.add(
                                    new LocaleInheritanceInfo(
                                            localeID, xpath, Reason.inheritanceMarker));
                        }
                        // skip the inheritance marker and keep going
                    } else {
                        // We have a “hard” value.
                        if (list == null) {
                            return new AliasLocation(xpath, localeID);
                        }
                        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                            list.add(
                                    new LocaleInheritanceInfo(
                                            localeID, xpath, Reason.inheritanceMarker));
                        } else {
                            list.add(new LocaleInheritanceInfo(localeID, xpath, Reason.value));
                        }
                        // Now, keep looping to add additional Bailey values.
                        // Note that we will typically exit the recursion (terminal state)
                        // with Reason.codeFallback or Reason.none
                        if (firstValue == null) {
                            // Save this, this will eventually be the function return.
                            firstValue = new AliasLocation(xpath, localeID);
                            // Everything else is only for Bailey.
                        } // else: we’re already looping.
                    }
                } else if (list != null) {
                    // No value, but we do have a list to update
                    // Note that the path wasn't found in this locale
                    // This also gives a trace of the locale inheritance
                    list.add(new LocaleInheritanceInfo(localeID, xpath, Reason.none));
                }
            }
            // Path not found, check if an alias exists
            final String rootAliasLocale = XMLSource.ROOT_ID; // Locale ID for aliases
            TreeMap<String, String> aliases = sources.get(rootAliasLocale).getAliases();
            String aliasedPath = aliases.get(xpath);

            if (aliasedPath == null) {
                // Check if there is an alias for a subset xpath.
                // If there are one or more matching aliases, lowerKey() will
                // return the alias with the longest matching prefix since the
                // hashmap is sorted according to xpath.

                //                // The following is a work in progress
                //                // We need to recurse, since we might have a chain of aliases
                //                while (true) {
                String possibleSubpath = aliases.lowerKey(xpath);
                if (possibleSubpath != null && xpath.startsWith(possibleSubpath)) {
                    aliasedPath =
                            aliases.get(possibleSubpath)
                                    + xpath.substring(possibleSubpath.length());
                    aliasedPath = aliasedPath.intern();
                    if (list != null) {
                        // It's an explicit alias, just at a parent element (subset xpath)
                        list.add(
                                new LocaleInheritanceInfo(
                                        rootAliasLocale, aliasedPath, Reason.alias));
                    }
                    //                        xpath = aliasedPath;
                    //                    } else {
                    //                        break;
                    //                    }
                }
            } else {
                if (list != null) {
                    // explicit, exact alias at this location
                    list.add(new LocaleInheritanceInfo(rootAliasLocale, aliasedPath, Reason.alias));
                }
            }

            // alts are special; they act like there is a root alias to the path without the alt.
            if (aliasedPath == null && xpath.contains("[@alt=")) {
                aliasedPath = XPathParts.getPathWithoutAlt(xpath).intern();
                if (list != null) {
                    list.add(
                            new LocaleInheritanceInfo(
                                    null, aliasedPath, Reason.removedAttribute, "alt"));
                }
            }

            // counts are special; they act like there is a root alias to 'other'
            // and in the special case of currencies, other => null
            // //ldml/numbers/currencies/currency[@type="BRZ"]/displayName[@count="other"] =>
            // //ldml/numbers/currencies/currency[@type="BRZ"]/displayName
            if (aliasedPath == null && xpath.contains("[@count=")) {
                aliasedPath = COUNT_EQUALS.matcher(xpath).replaceAll("[@count=\"other\"]").intern();
                if (aliasedPath.equals(xpath)) {
                    if (xpath.contains("/displayName")) {
                        aliasedPath = COUNT_EQUALS.matcher(xpath).replaceAll("").intern();
                        if (aliasedPath.equals(xpath)) {
                            throw new RuntimeException("Internal error");
                        }
                    } else {
                        // the replacement failed, do not alias
                        aliasedPath = null;
                    }
                }
                if (list != null && aliasedPath != null) {
                    // two different paths above reach here
                    list.add(
                            new LocaleInheritanceInfo(
                                    null, aliasedPath, Reason.changedAttribute, "count"));
                }
            }

            if (aliasedPath != null) {
                // Call getCachedFullStatus recursively to avoid recalculating cached aliases.
                AliasLocation cachedFullStatus =
                        getCachedFullStatus(aliasedPath, skipInheritanceMarker, list);
                // We call the above first, to update the list (if needed)
                if (firstValue == null) {
                    // not looping due to Bailey - return the cached status.
                    return cachedFullStatus;
                } else {
                    // Bailey loop. Return the first value.
                    return firstValue;
                }
            }

            // Fallback location.
            if (list != null) {
                // Not using CODE_FALLBACK_ID as it is implicit in the reason
                list.add(new LocaleInheritanceInfo(null, xpath, Reason.codeFallback));
            }
            if (firstValue == null) {
                return new AliasLocation(xpath, CODE_FALLBACK_ID);
            } else {
                return firstValue;
            }
        }

        /**
         * We have to go through the source, add all the paths, then recurse to parents However,
         * aliases are tricky, so watch it.
         */
        static final boolean TRACE_FILL = CldrUtility.getProperty("TRACE_FILL", false);

        static final String DEBUG_PATH_STRING = CldrUtility.getProperty("DEBUG_PATH", null);
        static final Pattern DEBUG_PATH =
                DEBUG_PATH_STRING == null ? null : PatternCache.get(DEBUG_PATH_STRING);
        static final boolean SKIP_FALLBACKID = CldrUtility.getProperty("SKIP_FALLBACKID", false);

        static final int MAX_LEVEL = 40; /* Throw an error if it goes past this. */

        /**
         * Initialises the set of xpaths that a fully resolved XMLSource contains.
         * http://cldr.unicode.org/development/development-process/design-proposals/resolution-of-cldr-files.
         * Information about the aliased path and source locale ID of each xpath is not
         * precalculated here since it doesn't appear to improve overall performance.
         */
        private Set<String> fillKeys() {
            Set<String> paths = findNonAliasedPaths();
            // Find aliased paths and loop until no more aliases can be found.
            Set<String> newPaths = paths;
            int level = 0;
            boolean newPathsFound = false;
            do {
                // Debugging code to protect against an infinite loop.
                if (TRACE_FILL && DEBUG_PATH == null || level > MAX_LEVEL) {
                    System.out.println(
                            Utility.repeat(TRACE_INDENT, level)
                                    + "# paths waiting to be aliased: "
                                    + newPaths.size());
                    System.out.println(
                            Utility.repeat(TRACE_INDENT, level) + "# paths found: " + paths.size());
                }
                if (level > MAX_LEVEL) throw new IllegalArgumentException("Stack overflow");

                String[] sortedPaths = new String[newPaths.size()];
                newPaths.toArray(sortedPaths);
                Arrays.sort(sortedPaths);

                newPaths = getDirectAliases(sortedPaths);
                newPathsFound = paths.addAll(newPaths);
                level++;
            } while (newPathsFound);
            return paths;
        }

        /**
         * Creates the set of resolved paths for this ResolvingSource while ignoring aliasing.
         *
         * @return
         */
        private Set<String> findNonAliasedPaths() {
            HashSet<String> paths = new HashSet<>();

            // Get all XMLSources used during resolution.
            List<XMLSource> sourceList = new ArrayList<>(sources.values());
            if (!SKIP_FALLBACKID) {
                sourceList.add(constructedItems);
            }

            // Make a pass through, filling all the direct paths, excluding aliases, and collecting
            // others
            for (XMLSource curSource : sourceList) {
                for (String xpath : curSource) {
                    paths.add(xpath.intern());
                }
            }
            return paths;
        }

        /**
         * Takes in a list of xpaths and returns a new set of paths that alias directly to those
         * existing xpaths.
         *
         * @param paths a sorted list of xpaths
         * @return the new set of paths
         */
        private Set<String> getDirectAliases(String[] paths) {
            HashSet<String> newPaths = new HashSet<>();
            // Keep track of the current path index: since it's sorted, we
            // never have to backtrack.
            int pathIndex = 0;
            LinkedHashMap<String, List<String>> reverseAliases = getReverseAliases();
            for (String subpath : reverseAliases.keySet()) {
                // Find the first path that matches the current alias.
                while (pathIndex < paths.length && paths[pathIndex].compareTo(subpath) < 0) {
                    pathIndex++;
                }

                // Alias all paths that match the current alias.
                String xpath;
                List<String> list = reverseAliases.get(subpath);
                int endIndex = pathIndex;
                int suffixStart = subpath.length();
                // Suffixes should always start with an element and not an
                // attribute to prevent invalid aliasing.
                while (endIndex < paths.length
                        && (xpath = paths[endIndex]).startsWith(subpath)
                        && xpath.charAt(suffixStart) == '/') {
                    String suffix = xpath.substring(suffixStart);
                    for (String reverseAlias : list) {
                        String reversePath = reverseAlias + suffix;
                        newPaths.add(reversePath.intern());
                    }
                    endIndex++;
                }
                if (endIndex == paths.length) break;
            }
            return newPaths;
        }

        private LinkedHashMap<String, List<String>> getReverseAliases() {
            return sources.get("root").getReverseAliases();
        }

        private transient Set<String> cachedKeySet = null;

        /**
         * @return an iterator over all the xpaths in this XMLSource.
         */
        @Override
        public Iterator<String> iterator() {
            return getCachedKeySet().iterator();
        }

        private Set<String> getCachedKeySet() {
            if (cachedKeySet == null) {
                cachedKeySet = fillKeys();
                cachedKeySet = Collections.unmodifiableSet(cachedKeySet);
            }
            return cachedKeySet;
        }

        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        @Override
        public Comments getXpathComments() {
            return currentSource.getXpathComments();
        }

        @Override
        public void setXpathComments(Comments path) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        @Override
        public void removeValueAtDPath(String xpath) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are  read-only");
        }

        @Override
        public XMLSource freeze() {
            return this; // No-op. ResolvingSource is already read-only.
        }

        @Override
        public boolean isFrozen() {
            return true; // ResolvingSource is already read-only.
        }

        @Override
        public void valueChanged(String xpath, XMLSource nonResolvingSource) {
            if (!cachingIsEnabled) {
                return;
            }
            synchronized (getSourceLocaleIDCache) {
                AliasLocation location = getSourceLocaleIDCache.remove(xpath);
                if (location == null) {
                    return;
                }
                // Paths aliasing to this path (directly or indirectly) may be affected,
                // so clear them as well.
                // There's probably a more elegant way to fix the paths than simply
                // throwing everything out.
                Set<String> dependentPaths = getDirectAliases(new String[] {xpath});
                if (dependentPaths.size() > 0) {
                    for (String path : dependentPaths) {
                        getSourceLocaleIDCache.remove(path);
                    }
                }
            }
        }

        /**
         * Creates a new ResolvingSource with the given locale resolution chain.
         *
         * @param sourceList the list of XMLSources to look in during resolution, ordered from the
         *     current locale up to root.
         */
        public ResolvingSource(List<XMLSource> sourceList) {
            // Sanity check for root.
            if (sourceList == null
                    || !sourceList.get(sourceList.size() - 1).getLocaleID().equals("root")) {
                throw new IllegalArgumentException("Last element should be root");
            }
            currentSource = sourceList.get(0); // Convenience variable
            sources = new LinkedHashMap<>();
            for (XMLSource source : sourceList) {
                sources.put(source.getLocaleID(), source);
            }

            // Add listeners to all locales except root, since we don't expect
            // root to change programatically.
            for (int i = 0, limit = sourceList.size() - 1; i < limit; i++) {
                sourceList.get(i).addListener(this);
            }
        }

        @Override
        public String getLocaleID() {
            return currentSource.getLocaleID();
        }

        private static final String[] keyDisplayNames = {
            "calendar", "cf", "collation", "currency", "hc", "lb", "ms", "numbers"
        };
        private static final String[][] typeDisplayNames = {
            {"account", "cf"},
            {"ahom", "numbers"},
            {"arab", "numbers"},
            {"arabext", "numbers"},
            {"armn", "numbers"},
            {"armnlow", "numbers"},
            {"bali", "numbers"},
            {"beng", "numbers"},
            {"big5han", "collation"},
            {"brah", "numbers"},
            {"buddhist", "calendar"},
            {"cakm", "numbers"},
            {"cham", "numbers"},
            {"chinese", "calendar"},
            {"compat", "collation"},
            {"coptic", "calendar"},
            {"cyrl", "numbers"},
            {"dangi", "calendar"},
            {"deva", "numbers"},
            {"diak", "numbers"},
            {"dictionary", "collation"},
            {"ducet", "collation"},
            {"emoji", "collation"},
            {"eor", "collation"},
            {"ethi", "numbers"},
            {"ethiopic", "calendar"},
            {"ethiopic-amete-alem", "calendar"},
            {"fullwide", "numbers"},
            {"gb2312han", "collation"},
            {"geor", "numbers"},
            {"gong", "numbers"},
            {"gonm", "numbers"},
            {"gregorian", "calendar"},
            {"grek", "numbers"},
            {"greklow", "numbers"},
            {"gujr", "numbers"},
            {"guru", "numbers"},
            {"h11", "hc"},
            {"h12", "hc"},
            {"h23", "hc"},
            {"h24", "hc"},
            {"hanidec", "numbers"},
            {"hans", "numbers"},
            {"hansfin", "numbers"},
            {"hant", "numbers"},
            {"hantfin", "numbers"},
            {"hebr", "numbers"},
            {"hebrew", "calendar"},
            {"hmng", "numbers"},
            {"hmnp", "numbers"},
            {"indian", "calendar"},
            {"islamic", "calendar"},
            {"islamic-civil", "calendar"},
            {"islamic-rgsa", "calendar"},
            {"islamic-tbla", "calendar"},
            {"islamic-umalqura", "calendar"},
            {"iso8601", "calendar"},
            {"japanese", "calendar"},
            {"java", "numbers"},
            {"jpan", "numbers"},
            {"jpanfin", "numbers"},
            {"kali", "numbers"},
            {"kawi", "numbers"},
            {"khmr", "numbers"},
            {"knda", "numbers"},
            {"lana", "numbers"},
            {"lanatham", "numbers"},
            {"laoo", "numbers"},
            {"latn", "numbers"},
            {"lepc", "numbers"},
            {"limb", "numbers"},
            {"loose", "lb"},
            {"mathbold", "numbers"},
            {"mathdbl", "numbers"},
            {"mathmono", "numbers"},
            {"mathsanb", "numbers"},
            {"mathsans", "numbers"},
            {"metric", "ms"},
            {"mlym", "numbers"},
            {"modi", "numbers"},
            {"mong", "numbers"},
            {"mroo", "numbers"},
            {"mtei", "numbers"},
            {"mymr", "numbers"},
            {"mymrshan", "numbers"},
            {"mymrtlng", "numbers"},
            {"nagm", "numbers"},
            {"nkoo", "numbers"},
            {"normal", "lb"},
            {"olck", "numbers"},
            {"orya", "numbers"},
            {"osma", "numbers"},
            {"persian", "calendar"},
            {"phonebook", "collation"},
            {"pinyin", "collation"},
            {"reformed", "collation"},
            {"roc", "calendar"},
            {"rohg", "numbers"},
            {"roman", "numbers"},
            {"romanlow", "numbers"},
            {"saur", "numbers"},
            {"search", "collation"},
            {"searchjl", "collation"},
            {"shrd", "numbers"},
            {"sind", "numbers"},
            {"sinh", "numbers"},
            {"sora", "numbers"},
            {"standard", "cf"},
            {"standard", "collation"},
            {"strict", "lb"},
            {"stroke", "collation"},
            {"sund", "numbers"},
            {"takr", "numbers"},
            {"talu", "numbers"},
            {"taml", "numbers"},
            {"tamldec", "numbers"},
            {"tnsa", "numbers"},
            {"telu", "numbers"},
            {"thai", "numbers"},
            {"tibt", "numbers"},
            {"tirh", "numbers"},
            {"traditional", "collation"},
            {"unihan", "collation"},
            {"uksystem", "ms"},
            {"ussystem", "ms"},
            {"vaii", "numbers"},
            {"wara", "numbers"},
            {"wcho", "numbers"},
            {"zhuyin", "collation"}
        };

        private static final boolean SKIP_SINGLEZONES = false;
        private static XMLSource constructedItems = new SimpleXMLSource(CODE_FALLBACK_ID);

        static {
            StandardCodes sc = StandardCodes.make();
            Map<String, Set<String>> countries_zoneSet = sc.getCountryToZoneSet();
            Map<String, String> zone_countries = sc.getZoneToCounty();

            for (int typeNo = 0; typeNo <= CLDRFile.TZ_START; ++typeNo) {
                String type = CLDRFile.getNameName(typeNo);
                String type2 =
                        (typeNo == CLDRFile.CURRENCY_SYMBOL)
                                ? CLDRFile.getNameName(CLDRFile.CURRENCY_NAME)
                                : (typeNo >= CLDRFile.TZ_START) ? "tzid" : type;
                Set<String> codes = sc.getSurveyToolDisplayCodes(type2);
                for (Iterator<String> codeIt = codes.iterator(); codeIt.hasNext(); ) {
                    String code = codeIt.next();
                    String value = code;
                    if (typeNo == CLDRFile.TZ_EXEMPLAR) { // skip single-zone countries
                        if (SKIP_SINGLEZONES) {
                            String country = zone_countries.get(code);
                            Set<String> s = countries_zoneSet.get(country);
                            if (s != null && s.size() == 1) continue;
                        }
                        value = TimezoneFormatter.getFallbackName(value);
                    } else if (typeNo == CLDRFile.LANGUAGE_NAME) {
                        if (ROOT_ID.equals(value)) {
                            continue;
                        }
                    }
                    addFallbackCode(typeNo, code, value);
                }
            }

            String[] extraCodes = {
                "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US", "es_419", "es_ES",
                "es_MX", "fa_AF", "fr_CA", "fr_CH", "frc", "hi_Latn", "lou", "nds_NL", "nl_BE",
                "pt_BR", "pt_PT", "ro_MD", "sw_CD", "zh_Hans", "zh_Hant"
            };
            for (String extraCode : extraCodes) {
                addFallbackCode(CLDRFile.LANGUAGE_NAME, extraCode, extraCode);
            }

            addFallbackCode(CLDRFile.LANGUAGE_NAME, "en_GB", "en_GB", "short");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "en_US", "en_US", "short");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "az", "az", "short");

            addFallbackCode(CLDRFile.LANGUAGE_NAME, "ckb", "ckb", "menu");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "ckb", "ckb", "variant");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "hi_Latn", "hi_Latn", "variant");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "yue", "yue", "menu");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "zh", "zh", "menu");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "zh_Hans", "zh", "long");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "zh_Hant", "zh", "long");

            addFallbackCode(CLDRFile.SCRIPT_NAME, "Hans", "Hans", "stand-alone");
            addFallbackCode(CLDRFile.SCRIPT_NAME, "Hant", "Hant", "stand-alone");

            addFallbackCode(CLDRFile.TERRITORY_NAME, "GB", "GB", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "HK", "HK", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "MO", "MO", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "PS", "PS", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "US", "US", "short");

            addFallbackCode(
                    CLDRFile.TERRITORY_NAME, "CD", "CD", "variant"); // add other geopolitical items
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CG", "CG", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CI", "CI", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CZ", "CZ", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "FK", "FK", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "TL", "TL", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "SZ", "SZ", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "IO", "IO", "biot");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "IO", "IO", "chagos");

            // new alternate name

            addFallbackCode(CLDRFile.TERRITORY_NAME, "NZ", "NZ", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "TR", "TR", "variant");

            addFallbackCode(CLDRFile.TERRITORY_NAME, "XA", "XA");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "XB", "XB");

            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"0\"]",
                    "BCE",
                    "variant");
            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"1\"]",
                    "CE",
                    "variant");
            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"0\"]",
                    "BCE",
                    "variant");
            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"1\"]",
                    "CE",
                    "variant");
            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"0\"]",
                    "BCE",
                    "variant");
            addFallbackCode(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"1\"]",
                    "CE",
                    "variant");

            for (int i = 0; i < keyDisplayNames.length; ++i) {
                constructedItems.putValueAtPath(
                        "//ldml/localeDisplayNames/keys/key"
                                + "[@type=\""
                                + keyDisplayNames[i]
                                + "\"]",
                        keyDisplayNames[i]);
            }
            for (int i = 0; i < typeDisplayNames.length; ++i) {
                constructedItems.putValueAtPath(
                        "//ldml/localeDisplayNames/types/type"
                                + "[@key=\""
                                + typeDisplayNames[i][1]
                                + "\"]"
                                + "[@type=\""
                                + typeDisplayNames[i][0]
                                + "\"]",
                        typeDisplayNames[i][0]);
            }
            constructedItems.freeze();
            allowDuplicates = Collections.unmodifiableMap(allowDuplicates);
        }

        private static void addFallbackCode(int typeNo, String code, String value) {
            addFallbackCode(typeNo, code, value, null);
        }

        private static void addFallbackCode(int typeNo, String code, String value, String alt) {
            String fullpath = CLDRFile.getKey(typeNo, code);
            String distinguishingPath = addFallbackCodeToConstructedItems(fullpath, value, alt);
            if (typeNo == CLDRFile.LANGUAGE_NAME
                    || typeNo == CLDRFile.SCRIPT_NAME
                    || typeNo == CLDRFile.TERRITORY_NAME) {
                allowDuplicates.put(distinguishingPath, code);
            }
        }

        private static void addFallbackCode(
                String fullpath, String value, String alt) { // assumes no allowDuplicates for this
            addFallbackCodeToConstructedItems(fullpath, value, alt); // ignore unneeded return value
        }

        private static String addFallbackCodeToConstructedItems(
                String fullpath, String value, String alt) {
            if (alt != null) {
                // Insert the @alt= string after the last occurrence of "]"
                StringBuffer fullpathBuf = new StringBuffer(fullpath);
                fullpath =
                        fullpathBuf
                                .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                                .toString();
            }
            return constructedItems.putValueAtPath(fullpath, value);
        }

        @Override
        public boolean isHere(String path) {
            return currentSource.isHere(path); // only test one level
        }

        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            // NOTE: No caching is currently performed here because the unresolved
            // locales already cache their value-path mappings, and it's not
            // clear yet how much further caching would speed this up.

            // Add all non-aliased paths with the specified value.
            List<XMLSource> children = new ArrayList<>();
            Set<String> filteredPaths = new HashSet<>();
            for (XMLSource source : sources.values()) {
                Set<String> pathsWithValue = new HashSet<>();
                source.getPathsWithValue(valueToMatch, pathPrefix, pathsWithValue);
                // Don't add a path with the value if it is overridden by a child locale.
                for (String pathWithValue : pathsWithValue) {
                    if (!sourcesHavePath(pathWithValue, children)) {
                        filteredPaths.add(pathWithValue);
                    }
                }
                children.add(source);
            }

            // Find all paths that alias to the specified value, then filter by
            // path prefix.
            Set<String> aliases = new HashSet<>();
            Set<String> oldAliases = new HashSet<>(filteredPaths);
            Set<String> newAliases;
            do {
                String[] sortedPaths = new String[oldAliases.size()];
                oldAliases.toArray(sortedPaths);
                Arrays.sort(sortedPaths);
                newAliases = getDirectAliases(sortedPaths);
                oldAliases = newAliases;
                aliases.addAll(newAliases);
            } while (newAliases.size() > 0);

            // get the aliases, but only the ones that have values that match
            String norm = null;
            for (String alias : aliases) {
                if (alias.startsWith(pathPrefix)) {
                    if (norm == null && valueToMatch != null) {
                        norm = SimpleXMLSource.normalize(valueToMatch);
                    }
                    String value = getValueAtDPath(alias);
                    if (value != null && SimpleXMLSource.normalize(value).equals(norm)) {
                        filteredPaths.add(alias);
                    }
                }
            }

            result.addAll(filteredPaths);
        }

        private boolean sourcesHavePath(String xpath, List<XMLSource> sources) {
            for (XMLSource source : sources) {
                if (source.hasValueAtDPath(xpath)) return true;
            }
            return false;
        }

        @Override
        public VersionInfo getDtdVersionInfo() {
            return currentSource.getDtdVersionInfo();
        }
    }

    /**
     * See CLDRFile isWinningPath for documentation
     *
     * @param path
     * @return
     */
    public boolean isWinningPath(String path) {
        return getWinningPath(path).equals(path);
    }

    /**
     * See CLDRFile getWinningPath for documentation. Default implementation is that it removes
     * draft and [@alt="...proposed..." if possible
     *
     * @param path
     * @return
     */
    public String getWinningPath(String path) {
        String newPath = CLDRFile.getNondraftNonaltXPath(path);
        if (!newPath.equals(path)) {
            String value = getValueAtPath(newPath); // ensure that it still works
            if (value != null) {
                return newPath;
            }
        }
        return path;
    }

    /** Adds a listener to this XML source. */
    public void addListener(Listener listener) {
        listeners.add(new WeakReference<>(listener));
    }

    /**
     * Notifies all listeners that the winning value for the given path has changed.
     *
     * @param xpath the xpath where the change occurred.
     */
    public void notifyListeners(String xpath) {
        int i = 0;
        while (i < listeners.size()) {
            Listener listener = listeners.get(i).get();
            if (listener == null) { // listener has been garbage-collected.
                listeners.remove(i);
            } else {
                listener.valueChanged(xpath, this);
                i++;
            }
        }
    }

    /**
     * return true if the path in this file (without resolution). Default implementation is to just
     * see if the path has a value. The resolved source must just test the top level.
     *
     * @param path
     * @return
     */
    public boolean isHere(String path) {
        return getValueAtPath(path) != null;
    }

    /**
     * Find all the distinguished paths having values matching valueToMatch, and add them to result.
     *
     * @param valueToMatch
     * @param pathPrefix
     * @param result
     */
    public abstract void getPathsWithValue(
            String valueToMatch, String pathPrefix, Set<String> result);

    public VersionInfo getDtdVersionInfo() {
        return null;
    }

    @SuppressWarnings("unused")
    public String getBaileyValue(
            String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        return null; // only a resolving xmlsource will return a value
    }

    // HACK, should be field on XMLSource
    public DtdType getDtdType() {
        final Iterator<String> it = iterator();
        if (it.hasNext()) {
            String path = it.next();
            return DtdType.fromPath(path);
        }
        return null;
    }

    /** XMLNormalizingDtdType is set in XMLNormalizingHandler loading XML process */
    private DtdType XMLNormalizingDtdType;

    private static final boolean LOG_PROGRESS = false;

    public DtdType getXMLNormalizingDtdType() {
        return this.XMLNormalizingDtdType;
    }

    public void setXMLNormalizingDtdType(DtdType dtdType) {
        this.XMLNormalizingDtdType = dtdType;
    }

    /**
     * Sets the initial comment, replacing everything that was there Use in XMLNormalizingHandler
     * only
     */
    public XMLSource setInitialComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        Log.logln(LOG_PROGRESS, "SET initial Comment: \t" + comment);
        this.getXpathComments().setInitialComment(comment);
        return this;
    }

    /** Use in XMLNormalizingHandler only */
    public XMLSource addComment(String xpath, String comment, Comments.CommentType type) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        Log.logln(LOG_PROGRESS, "ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
        if (xpath == null || xpath.length() == 0) {
            this.getXpathComments()
                    .setFinalComment(
                            CldrUtility.joinWithSeparation(
                                    this.getXpathComments().getFinalComment(),
                                    XPathParts.NEWLINE,
                                    comment));
        } else {
            xpath = CLDRFile.getDistinguishingXPath(xpath, null);
            this.getXpathComments().addComment(type, xpath, comment);
        }
        return this;
    }

    /** Use in XMLNormalizingHandler only */
    public String getFullXPath(String xpath) {
        if (xpath == null) {
            throw new NullPointerException("Null distinguishing xpath");
        }
        String result = this.getFullPath(xpath);
        return result != null
                ? result
                : xpath; // we can't add any non-distinguishing values if there is nothing there.
    }

    /** Add a new element to a XMLSource Use in XMLNormalizingHandler only */
    public XMLSource add(String currentFullXPath, String value) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        Log.logln(
                LOG_PROGRESS,
                "ADDING: \t" + currentFullXPath + " \t" + value + "\t" + currentFullXPath);
        try {
            this.putValueAtPath(currentFullXPath.intern(), value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "failed adding " + currentFullXPath + ",\t" + value, e);
        }
        return this;
    }

    /**
     * Get frozen normalized XMLSource
     *
     * @param localeId
     * @param dirs
     * @param minimalDraftStatus
     * @return XMLSource
     */
    public static XMLSource getFrozenInstance(
            String localeId, List<File> dirs, DraftStatus minimalDraftStatus) {
        return XMLNormalizingLoader.getFrozenInstance(localeId, dirs, minimalDraftStatus);
    }

    /**
     * Add a SourceLocation to this full XPath. Base implementation does nothing.
     *
     * @param currentFullXPath
     * @param location
     * @return
     */
    public XMLSource addSourceLocation(String currentFullXPath, SourceLocation location) {
        return this;
    }

    /**
     * Get the SourceLocation for a specific XPath. Base implementation always returns null.
     *
     * @param fullXPath
     * @return
     */
    public SourceLocation getSourceLocation(String fullXPath) {
        return null;
    }
}
