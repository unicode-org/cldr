/*
 ******************************************************************************
 * Copyright (C) 2005-2011, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.util;

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

import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;

/**
 * Overall process is described in
 * http://cldr.unicode.org/development/development-process/design-proposals/resolution-of-cldr-files. Please update that
 * document if major
 * changes are made.
 */
public abstract class XMLSource implements Freezable<XMLSource>, Iterable<String> {
    public static final String CODE_FALLBACK_ID = "code-fallback";
    public static final String ROOT_ID = "root";
    public static final boolean USE_PARTS_IN_ALIAS = false;
    private static final String TRACE_INDENT = " "; // "\t"
    private transient XPathParts parts = new XPathParts(null, null);
    private static Map<String, String> allowDuplicates = new HashMap<String, String>();

    private String localeID;
    private boolean nonInheriting;
    private TreeMap<String, String> aliases;
    private LinkedHashMap<String, List<String>> reverseAliases;
    protected boolean locked;
    transient String[] fixedPath = new String[1];

    public static class AliasLocation {
        public final String pathWhereFound;
        public final String localeWhereFound;

        public AliasLocation(String pathWhereFound, String localeWhereFound) {
            this.pathWhereFound = pathWhereFound;
            this.localeWhereFound = localeWhereFound;
        }
    }

    // Listeners are stored using weak references so that they can be garbage collected.
    private List<WeakReference<Listener>> listeners = new ArrayList<WeakReference<Listener>>();

    public String getLocaleID() {
        return localeID;
    }

    public void setLocaleID(String localeID) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        this.localeID = localeID;
    }

    /**
     * Adds all the path,value pairs in tempMap.
     * The paths must be Full Paths.
     *
     * @param tempMap
     * @param conflict_resolution
     */
    public void putAll(Map<String, String> tempMap, int conflict_resolution) {
        for (Iterator<String> it = tempMap.keySet().iterator(); it.hasNext();) {
            String path = it.next();
            if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValueAtPath(path) != null) continue;
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
        for (Iterator<String> it = otherSource.iterator(); it.hasNext();) {
            String path = it.next();
            final String oldValue = getValueAtDPath(path);
            if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && oldValue != null) {
                continue;
            }
            final String newValue = otherSource.getValueAtDPath(path);
            if (newValue.equals(oldValue)) {
                continue;
            }
            putValueAtPath(otherSource.getFullPathAtDPath(path), newValue);
        }
    }

    /**
     * Removes all the paths in the collection.
     * WARNING: must be distinguishedPaths
     *
     * @param xpaths
     */
    public void removeAll(Collection<String> xpaths) {
        for (Iterator<String> it = xpaths.iterator(); it.hasNext();) {
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
        if (path == null) return false;
        if (fullpath.indexOf("[@draft=") < 0) return false;
        return parts.set(fullpath).containsAttribute("draft");
    }

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
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath, nonInheriting);
        putValueAtDPath(distinguishingXPath, value);
        if (!fixedPath[0].equals(distinguishingXPath)) {
            clearCache();
            putFullPathAtDPath(distinguishingXPath, fixedPath[0]);
        }
        return distinguishingXPath;
    }

    /**
     * Gets those paths that allow duplicates
     */

    public static Map<String, String> getPathsAllowingDuplicates() {
        return allowDuplicates;
    }

    /**
     * A listener for XML source data.
     */
    public static interface Listener {
        /**
         * Called whenever the source being listened to has a data change.
         *
         * @param xpath
         *            The xpath that had its value changed.
         * @param source
         *            back-pointer to the source that changed
         */
        public void valueChanged(String xpath, XMLSource source);
    }

    /**
     * Internal class. Immutable!
     */
    public static final class Alias {
        // public String oldLocaleID;
        final private String newLocaleID;
        final private String oldPath;
        final private String newPath;
        final private boolean pathsEqual;
        static final Pattern aliasPattern = Pattern
            .compile("(?:\\[@source=\"([^\"]*)\"])?(?:\\[@path=\"([^\"]*)\"])?(?:\\[@draft=\"([^\"]*)\"])?"); // constant,

        // so no
        // need to
        // sync

        public static Alias make(String aliasPath) {
            int pos = aliasPath.indexOf("/alias");
            if (pos < 0) return null; // quickcheck
            String aliasParts = aliasPath.substring(pos + 6);
            String oldPath = aliasPath.substring(0, pos);
            String newPath = null;

            return new Alias(pos, oldPath, newPath, aliasParts);
        }

        /**
         * @param newLocaleID
         * @param oldPath
         * @param aliasParts
         * @param newPath
         * @param pathsEqual
         */
        private Alias(int pos, String oldPath, String newPath, String aliasParts) {
            // if (USE_PARTS_IN_ALIAS) {
            // XPathParts tempAliasParts = new XPathParts(null, null);
            // if (!tempAliasParts.set(aliasPath).containsElement("alias")) {
            // return null;
            // }
            // Map attributes = tempAliasParts.getAttributes(tempAliasParts.size()-1);
            // result.newLocaleID = (String) attributes.get("source");
            // relativePath = (String) attributes.get("path");
            // if (result.newLocaleID != null && result.newLocaleID.equals("locale")) {
            // result.newLocaleID = null;
            // }
            // if (relativePath == null) {
            // result.newPath = result.oldPath;
            // }
            // else {
            // result.newPath = tempAliasParts.trimLast().addRelative(relativePath).toString();
            // }
            // } else {
            // do the same as the above with a regex
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

            // if (false) { // test
            // if (newLocaleID != null) {
            // if (!newLocaleID.equals(result.newLocaleID)) {
            // throw new IllegalArgumentException();
            // }
            // } else if (result.newLocaleID != null) {
            // throw new IllegalArgumentException();
            // }
            // if (!relativePath2.equals(relativePath)) {
            // throw new IllegalArgumentException();
            // }
            // if (!newPath.equals(result.newPath)) {
            // throw new IllegalArgumentException();
            // }
            // }
            // }

            boolean pathsEqual = oldPath.equals(newPath);

            if (pathsEqual && newLocaleID == null) {
                throw new IllegalArgumentException("Alias must have different path or different source. AliasPath: "
                    + aliasParts
                    + ", Alias: " + newPath + ", " + newLocaleID);
            }

            this.newLocaleID = newLocaleID;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.pathsEqual = pathsEqual;
        }

        /**
         * Create a new path from an old path + relative portion.
         * Basically, each ../ at the front of the relative portion removes a trailing
         * element+attributes from the old path.
         * WARNINGS:
         * 1. It could fail if an attribute value contains '/'. This should not be the
         * case except in alias elements, but need to verify.
         * 2. Also assumes that there are no extra /'s in the relative or old path.
         * 3. If we verified that the relative paths always used " in place of ',
         * we could also save a step.
         *
         * Maybe we could clean up #2 and #3 when reading in a CLDRFile the first time?
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

        // static final String ATTRIBUTE_PATTERN = "\\[@([^=]+)=\"([^\"]*)\"]";
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

        public String toString() {
            return
            // "oldLocaleID: " + oldLocaleID + ", " +
            "newLocaleID: " + newLocaleID + ",\t"
                +
                "oldPath: " + oldPath + ",\n\t"
                +
                "newPath: " + newPath;
        }

        /**
         * This function is called on the full path, when we know the distinguishing path matches the oldPath.
         * So we just want to modify the base of the path
         *
         * @param oldPath
         * @param newPath
         * @param result
         * @return
         */
        public String changeNewToOld(String fullPath, String newPath, String oldPath) {
            // do common case quickly
            if (fullPath.startsWith(newPath)) {
                return oldPath + fullPath.substring(newPath.length());
            }

            // fullPath will be the same as newPath, except for some attributes at the end.
            // add those attributes to oldPath, starting from the end.
            XPathParts partsOld = new XPathParts();
            XPathParts partsNew = new XPathParts();
            XPathParts partsFull = new XPathParts();
            partsOld.set(oldPath);
            partsNew.set(newPath);
            partsFull.set(fullPath);
            Map<String, String> attributesFull = partsFull.getAttributes(-1);
            Map<String, String> attributesNew = partsNew.getAttributes(-1);
            Map<String, String> attributesOld = partsOld.getAttributes(-1);
            for (Iterator<String> it = attributesFull.keySet().iterator(); it.hasNext();) {
                String attribute = it.next();
                if (attributesNew.containsKey(attribute)) continue;
                attributesOld.put(attribute, attributesFull.get(attribute));
            }
            String result = partsOld.toString();

            // for now, just assume check that there are no goofy bits
            // if (!fullPath.startsWith(newPath)) {
            // if (false) {
            // throw new IllegalArgumentException("Failure to fix path. "
            // + Utility.LINE_SEPARATOR + "\tfullPath: " + fullPath
            // + Utility.LINE_SEPARATOR + "\toldPath: " + oldPath
            // + Utility.LINE_SEPARATOR + "\tnewPath: " + newPath
            // );
            // }
            // String tempResult = oldPath + fullPath.substring(newPath.length());
            // if (!result.equals(tempResult)) {
            // System.err.println("fullPath: " + fullPath + Utility.LINE_SEPARATOR + "\toldPath: "
            // + oldPath + Utility.LINE_SEPARATOR + "\tnewPath: " + newPath
            // + Utility.LINE_SEPARATOR + "\tnewPath: " + result);
            // }
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
     * @return a mapping of paths to their aliases. Note that since root is the
     *         only locale to have aliases, all other locales will have no mappings.
     */
    protected synchronized TreeMap<String, String> getAliases() {
        // The cache assumes that aliases will never change over the lifetime of
        // an XMLSource.
        if (aliases == null) {
            aliases = new TreeMap<String, String>();
            // Look for aliases and create mappings for them.
            // Aliases are only ever found in root.
            for (String path : this) {
                if (!Alias.isAliasPath(path)) continue;
                String fullPath = getFullPathAtDPath(path);
                Alias temp = Alias.make(fullPath);
                if (temp == null) continue;
                aliases.put(temp.getOldPath(), temp.getNewPath());
            }
        }
        return aliases;
    }

    /**
     * @return a reverse mapping of aliases
     */
    private LinkedHashMap<String, List<String>> getReverseAliases() {
        if (reverseAliases != null) return reverseAliases;
        // Aliases are only ever found in root.
        Map<String, String> aliases = getAliases();
        Map<String, List<String>> reverse = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            List<String> list = reverse.get(entry.getValue());
            if (list == null) {
                list = new ArrayList<String>();
                reverse.put(entry.getValue(), list);
            }
            list.add(entry.getKey());
        }

        // Sort map.
        reverseAliases = new LinkedHashMap<String, List<String>>(new TreeMap<String, List<String>>(reverse));
        return reverseAliases;
    }

    /**
     * Clear any internal caches.
     */
    private void clearCache() {
        aliases = null;
    }

    /**
     * Return the localeID of the XMLSource where the path was found
     * SUBCLASSING: must be overridden in a resolving locale
     *
     * @param path
     * @param status
     *            TODO
     * @return
     */
    public String getSourceLocaleID(String path, CLDRFile.Status status) {
        if (status != null) {
            status.pathWhereFound = CLDRFile.getDistinguishingXPath(path, null, false);
        }
        return getLocaleID();
    }

    /**
     * Remove the value.
     * SUBCLASSING: must be overridden in a resolving locale
     *
     * @param xpath
     */
    public void removeValueAtPath(String xpath) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        clearCache();
        removeValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null, nonInheriting));
    }

    /**
     * Get the value.
     * SUBCLASSING: must be overridden in a resolving locale
     *
     * @param xpath
     * @return
     */
    public String getValueAtPath(String xpath) {
        return getValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null, nonInheriting));
    }

    /**
     * Get the full path for a distinguishing path
     * SUBCLASSING: must be overridden in a resolving locale
     *
     * @param xpath
     * @return
     */
    public String getFullPath(String xpath) {
        return getFullPathAtDPath(CLDRFile.getDistinguishingXPath(xpath, null, nonInheriting));
    }

    /**
     * Put the full path for this distinguishing path
     * The caller will have processed the path, and only call this with the distinguishing path
     * SUBCLASSING: must be overridden
     */
    abstract public void putFullPathAtDPath(String distinguishingXPath, String fullxpath);

    /**
     * Put the distinguishing path, value.
     * The caller will have processed the path, and only call this with the distinguishing path
     * SUBCLASSING: must be overridden
     */
    abstract public void putValueAtDPath(String distinguishingXPath, String value);

    /**
     * Remove the path, and the full path, and value corresponding to the path.
     * The caller will have processed the path, and only call this with the distinguishing path
     * SUBCLASSING: must be overridden
     */
    abstract public void removeValueAtDPath(String distinguishingXPath);

    /**
     * Get the value at the given distinguishing path
     * The caller will have processed the path, and only call this with the distinguishing path
     * SUBCLASSING: must be overridden
     */
    abstract public String getValueAtDPath(String path);

    public boolean hasValueAtDPath(String path) {
        return (getValueAtDPath(path) != null);
    }

    /**
     * Get the Last-Change Date (if known) when the value was changed.
     * SUBCLASSING: may be overridden. defaults to NULL.
     * @return last change date (if known), else null
     */
    public Date getChangeDateAtDPath(String path) {
        return null;
    }

    /**
     * Get the full path at the given distinguishing path
     * The caller will have processed the path, and only call this with the distinguishing path
     * SUBCLASSING: must be overridden
     */
    abstract public String getFullPathAtDPath(String path);

    /**
     * Get the comments for the source.
     * TODO: integrate the Comments class directly into this class
     * SUBCLASSING: must be overridden
     */
    abstract public Comments getXpathComments();

    /**
     * Set the comments for the source.
     * TODO: integrate the Comments class directly into this class
     * SUBCLASSING: must be overridden
     */
    abstract public void setXpathComments(Comments comments);

    /**
     * @return an iterator over the distinguished paths
     */
    abstract public Iterator<String> iterator();

    /**
     * @return an iterator over the distinguished paths that start with the prefix.
     *         SUBCLASSING: Normally overridden for efficiency
     */
    public Iterator<String> iterator(String prefix) {
        if (prefix == null || prefix.length() == 0) return iterator();
        return new com.ibm.icu.dev.util.CollectionUtilities.PrefixIterator().set(iterator(), prefix);
    }

    public Iterator<String> iterator(Matcher pathFilter) {
        if (pathFilter == null) return iterator();
        return new com.ibm.icu.dev.util.CollectionUtilities.RegexIterator().set(iterator(), pathFilter);
    }

    /**
     * @return returns whether resolving or not
     *         SUBCLASSING: Only changed for resolving subclasses
     */
    public boolean isResolving() {
        return false;
    }

    /**
     * Returns the unresolved version of this XMLSource.
     * SUBCLASSING: Override in resolving sources.
     */
    public XMLSource getUnresolving() {
        return this;
    }

    /**
     * SUBCLASSING: must be overridden
     */
    public XMLSource cloneAsThawed() {
        try {
            XMLSource result = (XMLSource) super.clone();
            result.locked = false;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("should never happen");
        }
    }

    /**
     * for debugging only
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = iterator(); it.hasNext();) {
            String path = it.next();
            String value = getValueAtDPath(path);
            String fullpath = getFullPathAtDPath(path);
            result.append(fullpath).append(" =\t ").append(value).append(CldrUtility.LINE_SEPARATOR);
        }
        return result.toString();
    }

    /**
     * for debugging only
     */
    public String toString(String regex) {
        Matcher matcher = PatternCache.get(regex).matcher("");
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = iterator(matcher); it.hasNext();) {
            String path = it.next();
            // if (!matcher.reset(path).matches()) continue;
            String value = getValueAtDPath(path);
            String fullpath = getFullPathAtDPath(path);
            result.append(fullpath).append(" =\t ").append(value).append(CldrUtility.LINE_SEPARATOR);
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
     *
     */
    public static class ResolvingSource extends XMLSource implements Listener {
        private XMLSource currentSource;
        private LinkedHashMap<String, XMLSource> sources;

        public boolean isResolving() {
            return true;
        }

        public XMLSource getUnresolving() {
            return sources.get(getLocaleID());
        }

        /*
         * If there is an alias, then inheritance gets tricky.
         * If there is a path //ldml/xyz/.../uvw/alias[@path=...][@source=...]
         * then the parent for //ldml/xyz/.../uvw/abc/.../def/
         * is source, and the path to search for is really: //ldml/xyz/.../uvw/path/abc/.../def/
         */
        public static final boolean TRACE_VALUE = CldrUtility.getProperty("TRACE_VALUE", false);;

        // Map<String,String> getValueAtDPathCache = new HashMap();

        public String getValueAtDPath(String xpath) {
            if (DEBUG_PATH != null && DEBUG_PATH.matcher(xpath).find()) {
                System.out.println("Getting value for Path: " + xpath);
            }
            if (TRACE_VALUE) System.out.println("\t*xpath: " + xpath
                + CldrUtility.LINE_SEPARATOR + "\t*source: " + currentSource.getClass().getName()
                + CldrUtility.LINE_SEPARATOR + "\t*locale: " + currentSource.getLocaleID());
            String result = null;
            AliasLocation fullStatus = getCachedFullStatus(xpath);
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

        public XMLSource getSource(AliasLocation fullStatus) {
            XMLSource source = sources.get(fullStatus.localeWhereFound);
            return source == null ? constructedItems : source;
        }

        // public String _getValueAtDPath(String xpath) {
        // XMLSource currentSource = mySource;
        // String result;
        // ParentAndPath parentAndPath = new ParentAndPath();
        //
        // parentAndPath.set(xpath, currentSource, getLocaleID()).next();
        // while (true) {
        // if (parentAndPath.parentID == null) {
        // return constructedItems.getValueAtDPath(xpath);
        // }
        // currentSource = make(parentAndPath.parentID); // factory.make(parentAndPath.parentID, false).dataSource;
        // if (TRACE_VALUE) System.out.println("xpath: " + parentAndPath.path
        // + Utility.LINE_SEPARATOR + "\tsource: " + currentSource.getClass().getName()
        // + Utility.LINE_SEPARATOR + "\tlocale: " + currentSource.getLocaleID()
        // );
        // result = currentSource.getValueAtDPath(parentAndPath.path);
        // if (result != null) {
        // if (TRACE_VALUE) System.out.println("result: " + result);
        // return result;
        // }
        // parentAndPath.next();
        // }
        // }

        Map<String, String> getFullPathAtDPathCache = new HashMap<String, String>();

        public String getFullPathAtDPath(String xpath) {
            String result = currentSource.getFullPathAtDPath(xpath);
            if (result != null) {
                return result;
            }
            // This is tricky. We need to find the alias location's path and full path.
            // then we need to the the non-distinguishing elements from them,
            // and add them into the requested path.
            AliasLocation fullStatus = getCachedFullStatus(xpath);
            if (fullStatus != null) {
                String fullPathWhereFound = getSource(fullStatus).getFullPathAtDPath(fullStatus.pathWhereFound);
                if (fullPathWhereFound == null) {
                    result = null;
                } else if (fullPathWhereFound.equals(fullStatus.pathWhereFound)) {
                    result = xpath; // no difference
                } else {
                    result = getFullPath(xpath, fullStatus, fullPathWhereFound);
                }
            }
            //
            // result = getFullPathAtDPathCache.get(xpath);
            // if (result == null) {
            // if (getCachedKeySet().contains(xpath)) {
            // result = _getFullPathAtDPath(xpath);
            // getFullPathAtDPathCache.put(xpath, result);
            // }
            // }
            return result;
        }

        @Override
        public Date getChangeDateAtDPath(String xpath) {
            Date result = currentSource.getChangeDateAtDPath(xpath);
            if (result != null) {
                return result;
            }
            AliasLocation fullStatus = getCachedFullStatus(xpath);
            if (fullStatus != null) {
                result = getSource(fullStatus).getChangeDateAtDPath(fullStatus.pathWhereFound);
            }
            return result;
        }

        private String getFullPath(String xpath, AliasLocation fullStatus, String fullPathWhereFound) {
            String result = getFullPathAtDPathCache.get(xpath);
            if (result == null) {
                // find the differences, and add them into xpath
                // we do this by walking through each element, adding the corresponding attribute values.
                // we add attributes FROM THE END, in case the lengths are different!
                XPathParts xpathParts = new XPathParts().set(xpath);
                XPathParts fullPathWhereFoundParts = new XPathParts().set(fullPathWhereFound);
                XPathParts pathWhereFoundParts = new XPathParts().set(fullStatus.pathWhereFound);
                int offset = xpathParts.size() - pathWhereFoundParts.size();

                for (int i = 0; i < pathWhereFoundParts.size(); ++i) {
                    Map<String, String> fullAttributes = fullPathWhereFoundParts.getAttributes(i);
                    Map<String, String> attributes = pathWhereFoundParts.getAttributes(i);
                    if (!attributes.equals(fullAttributes)) { // add differences
                        //Map<String, String> targetAttributes = xpathParts.getAttributes(i + offset);
                        for (String key : fullAttributes.keySet()) {
                            if (!attributes.containsKey(key)) {
                                String value = fullAttributes.get(key);
                                xpathParts.putAttributeValue(i + offset, key, value);
                            }
                        }
                    }
                }
                result = xpathParts.toString();
                getFullPathAtDPathCache.put(xpath, result);
            }
            return result;
        }

        /**
         * Return the value that would obtain if the value didn't exist.
         */
        @Override
        public String getBaileyValue(String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
            AliasLocation fullStatus = getPathLocation(xpath, true);
            if (localeWhereFound != null) {
                localeWhereFound.value = fullStatus.localeWhereFound;
            }
            if (pathWhereFound != null) {
                pathWhereFound.value = fullStatus.pathWhereFound;
            }
            return getSource(fullStatus).getValueAtDPath(fullStatus.pathWhereFound);
        }

        private AliasLocation getCachedFullStatus(String xpath) {
            synchronized (getSourceLocaleIDCache) {
                AliasLocation fullStatus = getSourceLocaleIDCache.get(xpath);
                if (fullStatus == null) {
                    fullStatus = getPathLocation(xpath, false);
                    getSourceLocaleIDCache.put(xpath, fullStatus); // cache copy
                }
                return fullStatus;
            }
        }

        // private String _getFullPathAtDPath(String xpath) {
        // String result = null;
        // XMLSource currentSource = mySource;
        // ParentAndPath parentAndPath = new ParentAndPath();
        // parentAndPath.set(xpath, currentSource, getLocaleID()).next();
        // while (true) {
        // if (parentAndPath.parentID == null) {
        // return constructedItems.getFullPathAtDPath(xpath);
        // }
        // currentSource = make(parentAndPath.parentID); // factory.make(parentAndPath.parentID, false).dataSource;
        // result = currentSource.getValueAtDPath(parentAndPath.path);
        // if (result != null) {
        // result = currentSource.getFullPathAtDPath(parentAndPath.path);
        // return tempAlias.changeNewToOld(result, parentAndPath.path, xpath);
        // }
        // parentAndPath.next();
        // }
        // }

        public String getWinningPath(String xpath) {
            String result = currentSource.getWinningPath(xpath);
            if (result != null) return result;
            AliasLocation fullStatus = getCachedFullStatus(xpath);
            if (fullStatus != null) {
                result = getSource(fullStatus).getWinningPath(fullStatus.pathWhereFound);
            } else {
                result = xpath;
            }
            //
            // result = getWinningPathCache.get(xpath);
            // if (result == null) {
            // if (!getCachedKeySet().contains(xpath)) {
            // return xpath;
            // }
            // result = _getWinningPath(xpath);
            // getWinningPathCache.put(xpath, result);
            // }
            return result;
        }

        // Map<String,String> getWinningPathCache = new HashMap();
        //
        // public String _getWinningPath(String xpath) {
        // XMLSource currentSource = mySource;
        // ParentAndPath parentAndPath = new ParentAndPath();
        // parentAndPath.set(xpath, currentSource, getLocaleID()).next();
        // while (true) {
        // if (parentAndPath.parentID == null) {
        // return xpath; // ran out of parents
        // //return constructedItems.getWinningPath(xpath);
        // }
        // currentSource = make(parentAndPath.parentID); // factory.make(parentAndPath.parentID, false).dataSource;
        // String result = currentSource.getWinningPath(parentAndPath.path);
        // if (result != null) {
        // return result;
        // }
        // parentAndPath.next();
        // }
        // }

        private transient Map<String, AliasLocation> getSourceLocaleIDCache = new WeakHashMap<String, AliasLocation>();

        public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
            AliasLocation fullStatus = getCachedFullStatus(distinguishedXPath);
            if (status != null) {
                status.pathWhereFound = fullStatus.pathWhereFound;
            }
            return fullStatus.localeWhereFound;
        }

        static final Pattern COUNT_EQUALS = PatternCache.get("\\[@count=\"[^\"]*\"]");

        private AliasLocation getPathLocation(String xpath, boolean skipFirst) {
            for (XMLSource source : sources.values()) {
                // allow the first source to be skipped, for george bailey value
                if (skipFirst) {
                    skipFirst = false;
                    continue;
                }
                if (source.hasValueAtDPath(xpath)) {
                    String value = source.getValueAtDPath(xpath);
                    if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                        continue;
                    }
                    return new AliasLocation(xpath, source.getLocaleID());
                }
            }
            // Path not found, check if an alias exists
            TreeMap<String, String> aliases = sources.get("root").getAliases();
            String aliasedPath = aliases.get(xpath);

            if (aliasedPath == null) {
                // Check if there is an alias for a subset xpath.
                // If there are one or more matching aliases, lowerKey() will
                // return the alias with the longest matching prefix since the
                // hashmap is sorted according to xpath.
                String possibleSubpath = aliases.lowerKey(xpath);
                if (possibleSubpath != null && xpath.startsWith(possibleSubpath)) {
                    aliasedPath = aliases.get(possibleSubpath) +
                        xpath.substring(possibleSubpath.length());
                }
            }

            // counts are special; they act like there is a root alias to 'other'
            // and in the special case of currencies, other => null
            // //ldml/numbers/currencies/currency[@type="BRZ"]/displayName[@count="other"] => //ldml/numbers/currencies/currency[@type="BRZ"]/displayName
            if (aliasedPath == null && xpath.contains("[@count=")) {
                aliasedPath = COUNT_EQUALS.matcher(xpath).replaceAll("[@count=\"other\"]");
                if (aliasedPath.equals(xpath)) {
                    if (xpath.contains("/displayName")) {
                        aliasedPath = COUNT_EQUALS.matcher(xpath).replaceAll("");
                        if (aliasedPath.equals(xpath)) {
                            throw new RuntimeException("Internal error");
                        }
                    } else {
                        aliasedPath = null;
                    }
                }
            }

            if (aliasedPath != null) {
                // Call getCachedFullStatus recursively to avoid recalculating cached aliases.
                return getCachedFullStatus(aliasedPath);
            }

            // Fallback location.
            return new AliasLocation(xpath, CODE_FALLBACK_ID);
        }

        /**
         * We have to go through the source, add all the paths, then recurse to parents
         * However, aliases are tricky, so watch it.
         */
        static final boolean TRACE_FILL = CldrUtility.getProperty("TRACE_FILL", false);
        static final String DEBUG_PATH_STRING = CldrUtility.getProperty("DEBUG_PATH", null);
        static final Pattern DEBUG_PATH = DEBUG_PATH_STRING == null ? null : PatternCache.get(DEBUG_PATH_STRING);
        static final boolean SKIP_FALLBACKID = CldrUtility.getProperty("SKIP_FALLBACKID", false);;

        static final int MAX_LEVEL = 40; /* Throw an error if it goes past this. */

        /**
         * Initialises the set of xpaths that a fully resolved XMLSource contains.
         * http://cldr.unicode.org/development/development-process/design-proposals/resolution-of-cldr-files.
         * Information about the aliased path and source locale ID of each xpath
         * is not precalculated here since it doesn't appear to improve overall
         * performance.
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
                    System.out.println(Utility.repeat(TRACE_INDENT, level) + "# paths waiting to be aliased: "
                        + newPaths.size());
                    System.out.println(Utility.repeat(TRACE_INDENT, level) + "# paths found: " + paths.size());
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
         * Creates the set of resolved paths for this ResolvingSource while
         * ignoring aliasing.
         *
         * @return
         */
        private Set<String> findNonAliasedPaths() {
            HashSet<String> paths = new HashSet<String>();

            // Get all XMLSources used during resolution.
            List<XMLSource> sourceList = new ArrayList<XMLSource>(sources.values());
            if (!SKIP_FALLBACKID) {
                sourceList.add(constructedItems);
            }

            // Make a pass through, filling all the direct paths, excluding aliases, and collecting others
            for (XMLSource curSource : sourceList) {
                for (String xpath : curSource) {
                    paths.add(xpath);
                }
            }
            return paths;
        }

        /**
         * Takes in a list of xpaths and returns a new set of paths that alias
         * directly to those existing xpaths.
         *
         * @param paths
         *            a sorted list of xpaths
         * @param reverseAliases
         *            a map of reverse aliases sorted by key.
         * @return
         */
        private Set<String> getDirectAliases(String[] paths) {
            HashSet<String> newPaths = new HashSet<String>();
            // Keep track of the current path index: since it's sorted, we
            // never have to backtrack.
            int pathIndex = 0;
            LinkedHashMap<String, List<String>> reverseAliases = getReverseAliases();
            for (String subpath : reverseAliases.keySet()) {
                // Find the first path that matches the current alias.
                while (pathIndex < paths.length &&
                    paths[pathIndex].compareTo(subpath) < 0) {
                    pathIndex++;
                }

                // Alias all paths that match the current alias.
                String xpath;
                List<String> list = reverseAliases.get(subpath);
                int endIndex = pathIndex;
                int suffixStart = subpath.length();
                // Suffixes should always start with an element and not an
                // attribute to prevent invalid aliasing.
                while (endIndex < paths.length &&
                    (xpath = paths[endIndex]).startsWith(subpath) &&
                    xpath.charAt(suffixStart) == '/') {
                    String suffix = xpath.substring(suffixStart);
                    for (String reverseAlias : list) {
                        String reversePath = reverseAlias + suffix;
                        newPaths.add(reversePath);
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
        public Iterator<String> iterator() {
            return getCachedKeySet().iterator();
        }

        private Set<String> getCachedKeySet() {
            if (cachedKeySet == null) {
                cachedKeySet = fillKeys();
                // System.out.println("CachedKeySet: " + cachedKeySet);
                // cachedKeySet.addAll(constructedItems.keySet());
                cachedKeySet = Collections.unmodifiableSet(cachedKeySet);
            }
            return cachedKeySet;
        }

        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        public void putValueAtDPath(String distinguishingXPath, String value) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        public Comments getXpathComments() {
            return currentSource.getXpathComments();
        }

        public void setXpathComments(Comments path) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
        }

        public void removeValueAtDPath(String xpath) {
            throw new UnsupportedOperationException("Resolved CLDRFiles are  read-only");
        }

        public XMLSource freeze() {
            return this; // No-op. ResolvingSource is already read-only.
        }

        @Override
        public void valueChanged(String xpath, XMLSource nonResolvingSource) {
            synchronized (getSourceLocaleIDCache) {
                AliasLocation location = getSourceLocaleIDCache.remove(xpath);
                if (location == null) return;
                // Paths aliasing to this path (directly or indirectly) may be affected,
                // so clear them as well.
                // There's probably a more elegant way to fix the paths than simply
                // throwing everything out.
                Set<String> dependentPaths = getDirectAliases(new String[] { xpath });
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
         * @param sourceList
         *            the list of XMLSources to look in during resolution,
         *            ordered from the current locale up to root.
         */
        public ResolvingSource(List<XMLSource> sourceList) {
            // Sanity check for root.
            if (sourceList == null || !sourceList.get(sourceList.size() - 1).getLocaleID().equals("root")) {
                throw new IllegalArgumentException("Last element should be root");
            }
            currentSource = sourceList.get(0); // Convenience variable
            sources = new LinkedHashMap<String, XMLSource>();
            for (XMLSource source : sourceList) {
                sources.put(source.getLocaleID(), source);
            }

            // Add listeners to all locales except root, since we don't expect
            // root to change programatically.
            for (int i = 0, limit = sourceList.size() - 1; i < limit; i++) {
                sourceList.get(i).addListener(this);
            }
        }

        public String getLocaleID() {
            return currentSource.getLocaleID();
        }

        private static final String[] keyDisplayNames = {
            "calendar",
            "cf",
            "collation",
            "currency",
            "hc",
            "lb",
            "ms",
            "numbers"
        };
        private static final String[][] typeDisplayNames = {
            { "account", "cf" },
            { "ahom", "numbers" },
            { "arab", "numbers" },
            { "arabext", "numbers" },
            { "armn", "numbers" },
            { "armnlow", "numbers" },
            { "bali", "numbers" },
            { "beng", "numbers" },
            { "big5han", "collation" },
            { "brah", "numbers" },
            { "buddhist", "calendar" },
            { "cakm", "numbers" },
            { "cham", "numbers" },
            { "chinese", "calendar" },
            { "compat", "collation" },
            { "coptic", "calendar" },
            { "cyrl", "numbers" },
            { "dangi", "calendar" },
            { "deva", "numbers" },
            { "dictionary", "collation" },
            { "ducet", "collation" },
            { "emoji", "collation" },
            { "eor", "collation" },
            { "ethi", "numbers" },
            { "ethiopic", "calendar" },
            { "ethiopic-amete-alem", "calendar" },
            { "fullwide", "numbers" },
            { "gb2312han", "collation" },
            { "geor", "numbers" },
            { "gong", "numbers" },
            { "gonm", "numbers" },
            { "gregorian", "calendar" },
            { "grek", "numbers" },
            { "greklow", "numbers" },
            { "gujr", "numbers" },
            { "guru", "numbers" },
            { "h11", "hc" },
            { "h12", "hc" },
            { "h23", "hc" },
            { "h24", "hc" },
            { "hanidec", "numbers" },
            { "hans", "numbers" },
            { "hansfin", "numbers" },
            { "hant", "numbers" },
            { "hantfin", "numbers" },
            { "hebr", "numbers" },
            { "hebrew", "calendar" },
            { "hmng", "numbers" },
            { "indian", "calendar" },
            { "islamic", "calendar" },
            { "islamic-civil", "calendar" },
            { "islamic-rgsa", "calendar" },
            { "islamic-tbla", "calendar" },
            { "islamic-umalqura", "calendar" },
            { "iso8601", "calendar" },
            { "japanese", "calendar" },
            { "java", "numbers" },
            { "jpan", "numbers" },
            { "jpanfin", "numbers" },
            { "kali", "numbers" },
            { "khmr", "numbers" },
            { "knda", "numbers" },
            { "lana", "numbers" },
            { "lanatham", "numbers" },
            { "laoo", "numbers" },
            { "latn", "numbers" },
            { "lepc", "numbers" },
            { "limb", "numbers" },
            { "loose", "lb" },
            { "mathbold", "numbers" },
            { "mathdbl", "numbers" },
            { "mathmono", "numbers" },
            { "mathsanb", "numbers" },
            { "mathsans", "numbers" },
            { "metric", "ms" },
            { "mlym", "numbers" },
            { "modi", "numbers" },
            { "mong", "numbers" },
            { "mroo", "numbers" },
            { "mtei", "numbers" },
            { "mymr", "numbers" },
            { "mymrshan", "numbers" },
            { "mymrtlng", "numbers" },
            { "nkoo", "numbers" },
            { "normal", "lb" },
            { "olck", "numbers" },
            { "orya", "numbers" },
            { "osma", "numbers" },
            { "persian", "calendar" },
            { "phonebook", "collation" },
            { "pinyin", "collation" },
            { "reformed", "collation" },
            { "roc", "calendar" },
            { "rohg", "numbers" },
            { "roman", "numbers" },
            { "romanlow", "numbers" },
            { "saur", "numbers" },
            { "search", "collation" },
            { "searchjl", "collation" },
            { "shrd", "numbers" },
            { "sind", "numbers" },
            { "sinh", "numbers" },
            { "sora", "numbers" },
            { "standard", "cf" },
            { "standard", "collation" },
            { "strict", "lb" },
            { "stroke", "collation" },
            { "sund", "numbers" },
            { "takr", "numbers" },
            { "talu", "numbers" },
            { "taml", "numbers" },
            { "tamldec", "numbers" },
            { "telu", "numbers" },
            { "thai", "numbers" },
            { "tibt", "numbers" },
            { "tirh", "numbers" },
            { "traditional", "collation" },
            { "unihan", "collation" },
            { "uksystem", "ms" },
            { "ussystem", "ms" },
            { "vaii", "numbers" },
            { "wara", "numbers" },
            { "zhuyin", "collation" } };

        private static final boolean SKIP_SINGLEZONES = false;
        private static XMLSource constructedItems = new SimpleXMLSource(CODE_FALLBACK_ID);

        static {
            StandardCodes sc = StandardCodes.make();
            Map<String, Set<String>> countries_zoneSet = sc.getCountryToZoneSet();
            Map<String, String> zone_countries = sc.getZoneToCounty();

            // Set types = sc.getAvailableTypes();
            for (int typeNo = 0; typeNo <= CLDRFile.TZ_START; ++typeNo) {
                String type = CLDRFile.getNameName(typeNo);
                // int typeNo = typeNameToCode(type);
                // if (typeNo < 0) continue;
                String type2 = (typeNo == CLDRFile.CURRENCY_SYMBOL) ? CLDRFile.getNameName(CLDRFile.CURRENCY_NAME)
                    : (typeNo >= CLDRFile.TZ_START) ? "tzid"
                        : type;
                Set<String> codes = sc.getSurveyToolDisplayCodes(type2);
                // String prefix = CLDRFile.NameTable[typeNo][0];
                // String postfix = CLDRFile.NameTable[typeNo][1];
                // String prefix2 = "//ldml" + prefix.substring(6); // [@version=\"" + GEN_VERSION + "\"]
                for (Iterator<String> codeIt = codes.iterator(); codeIt.hasNext();) {
                    String code = codeIt.next();
                    String value = code;
                    if (typeNo == CLDRFile.TZ_EXEMPLAR) { // skip single-zone countries
                        if (SKIP_SINGLEZONES) {
                            String country = (String) zone_countries.get(code);
                            Set<String> s = countries_zoneSet.get(country);
                            if (s != null && s.size() == 1) continue;
                        }
                        value = TimezoneFormatter.getFallbackName(value);
                    }
                    addFallbackCode(typeNo, code, value);
                }
            }

            // Add commonlyUsed
            // //ldml/dates/timeZoneNames/metazone[@type="New_Zealand"]/commonlyUsed
            // should get this from supplemental metadata, but for now...
            // String[] metazones =
            // "Acre Afghanistan Africa_Central Africa_Eastern Africa_FarWestern Africa_Southern Africa_Western Aktyubinsk Alaska Alaska_Hawaii Almaty Amazon America_Central America_Eastern America_Mountain America_Pacific Anadyr Aqtau Aqtobe Arabian Argentina Argentina_Western Armenia Ashkhabad Atlantic Australia_Central Australia_CentralWestern Australia_Eastern Australia_Western Azerbaijan Azores Baku Bangladesh Bering Bhutan Bolivia Borneo Brasilia British Brunei Cape_Verde Chamorro Changbai Chatham Chile China Choibalsan Christmas Cocos Colombia Cook Cuba Dacca Davis Dominican DumontDUrville Dushanbe Dutch_Guiana East_Timor Easter Ecuador Europe_Central Europe_Eastern Europe_Western Falkland Fiji French_Guiana French_Southern Frunze Gambier GMT Galapagos Georgia Gilbert_Islands Goose_Bay Greenland_Central Greenland_Eastern Greenland_Western Guam Gulf Guyana Hawaii_Aleutian Hong_Kong Hovd India Indian_Ocean Indochina Indonesia_Central Indonesia_Eastern Indonesia_Western Iran Irkutsk Irish Israel Japan Kamchatka Karachi Kashgar Kazakhstan_Eastern Kazakhstan_Western Kizilorda Korea Kosrae Krasnoyarsk Kuybyshev Kwajalein Kyrgystan Lanka Liberia Line_Islands Long_Shu Lord_Howe Macau Magadan Malaya Malaysia Maldives Marquesas Marshall_Islands Mauritius Mawson Mongolia Moscow Myanmar Nauru Nepal New_Caledonia New_Zealand Newfoundland Niue Norfolk North_Mariana Noronha Novosibirsk Omsk Oral Pakistan Palau Papua_New_Guinea Paraguay Peru Philippines Phoenix_Islands Pierre_Miquelon Pitcairn Ponape Qyzylorda Reunion Rothera Sakhalin Samara Samarkand Samoa Seychelles Shevchenko Singapore Solomon South_Georgia Suriname Sverdlovsk Syowa Tahiti Tajikistan Tashkent Tbilisi Tokelau Tonga Truk Turkey Turkmenistan Tuvalu Uralsk Uruguay Urumqi Uzbekistan Vanuatu Venezuela Vladivostok Volgograd Vostok Wake Wallis Yakutsk Yekaterinburg Yerevan Yukon".split("\\s+");
            // for (String metazone : metazones) {
            // constructedItems.putValueAtPath(
            // "//ldml/dates/timeZoneNames/metazone[@type=\""
            // + metazone
            // + "\"]/commonlyUsed",
            // "false");
            // }

            String[] extraCodes = { "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US", "es_419", "es_ES", "es_MX",
                "fr_CA", "fr_CH", "frc", "lou", "nds_NL", "nl_BE", "pt_BR", "pt_PT", "ro_MD", "sw_CD", "zh_Hans", "zh_Hant" };
            for (String extraCode : extraCodes) {
                addFallbackCode(CLDRFile.LANGUAGE_NAME, extraCode, extraCode);
            }


            addFallbackCode(CLDRFile.LANGUAGE_NAME, "en_GB", "en_GB", "short");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "en_US", "en_US", "short");
            addFallbackCode(CLDRFile.LANGUAGE_NAME, "az", "az", "short");

            addFallbackCode(CLDRFile.SCRIPT_NAME, "Hans", "Hans", "stand-alone");
            addFallbackCode(CLDRFile.SCRIPT_NAME, "Hant", "Hant", "stand-alone");

            addFallbackCode(CLDRFile.TERRITORY_NAME, "GB", "GB", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "HK", "HK", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "MO", "MO", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "PS", "PS", "short");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "US", "US", "short");

            addFallbackCode(CLDRFile.TERRITORY_NAME, "CD", "CD", "variant"); // add other geopolitical items
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CG", "CG", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CI", "CI", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "CZ", "CZ", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "FK", "FK", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "MK", "MK", "variant");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "TL", "TL", "variant");

            addFallbackCode(CLDRFile.TERRITORY_NAME, "XA", "XA");
            addFallbackCode(CLDRFile.TERRITORY_NAME, "XB", "XB");

            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"0\"]", "BCE", "variant");
            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"1\"]", "CE", "variant");
            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"0\"]", "BCE", "variant");
            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"1\"]", "CE", "variant");
            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"0\"]", "BCE", "variant");
            addFallbackCode("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"1\"]", "CE", "variant");

            //String defaultCurrPattern = "¤ #,##0.00"; // use root value; can't get the locale's currency pattern in this static context; "" and "∅∅∅" cause errors.
            for (int i = 0; i < keyDisplayNames.length; ++i) {
                constructedItems.putValueAtPath(
                    "//ldml/localeDisplayNames/keys/key" +
                        "[@type=\"" + keyDisplayNames[i] + "\"]",
                    keyDisplayNames[i]);
            }
            for (int i = 0; i < typeDisplayNames.length; ++i) {
                constructedItems.putValueAtPath(
                    "//ldml/localeDisplayNames/types/type"
                        + "[@key=\"" + typeDisplayNames[i][1] + "\"]"
                        + "[@type=\"" + typeDisplayNames[i][0] + "\"]",
                    typeDisplayNames[i][0]);
            }
            //            String[][] relativeValues = {
            //                // {"Three days ago", "-3"},
            //                { "The day before yesterday", "-2" },
            //                { "Yesterday", "-1" },
            //                { "Today", "0" },
            //                { "Tomorrow", "1" },
            //                { "The day after tomorrow", "2" },
            //                // {"Three days from now", "3"},
            //            };
            //            for (int i = 0; i < relativeValues.length; ++i) {
            //                constructedItems.putValueAtPath(
            //                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/fields/field[@type=\"day\"]/relative[@type=\""
            //                        + relativeValues[i][1] + "\"]",
            //                        relativeValues[i][0]);
            //            }

            constructedItems.freeze();
            allowDuplicates = Collections.unmodifiableMap(allowDuplicates);
            // System.out.println("constructedItems: " + constructedItems);
        }

        private static void addFallbackCode(int typeNo, String code, String value) {
            addFallbackCode(typeNo, code, value, null);
        }

        private static void addFallbackCode(int typeNo, String code, String value, String alt) {
            // String path = prefix + code + postfix;
            String fullpath = CLDRFile.getKey(typeNo, code);
            String distinguishingPath = addFallbackCodeToConstructedItems(fullpath, value, alt);
            if (typeNo == CLDRFile.LANGUAGE_NAME || typeNo == CLDRFile.SCRIPT_NAME || typeNo == CLDRFile.TERRITORY_NAME) {
                allowDuplicates.put(distinguishingPath, code);
            }
        }

        private static void addFallbackCode(String fullpath, String value, String alt) { // assumes no allowDuplicates for this
            addFallbackCodeToConstructedItems(fullpath, value, alt); // ignore unneeded return value
        }

        private static String addFallbackCodeToConstructedItems(String fullpath, String value, String alt) {
            if (alt != null) {
                // Insert the @alt= string after the last occurrence of "]"
                StringBuffer fullpathBuf = new StringBuffer(fullpath);
                fullpath = fullpathBuf.insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]").toString();
            }
            // System.out.println(fullpath + "\t=> " + code);
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
            List<XMLSource> children = new ArrayList<XMLSource>();
            Set<String> filteredPaths = new HashSet<String>();
            for (XMLSource source : sources.values()) {
                Set<String> pathsWithValue = new HashSet<String>();
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
            Set<String> aliases = new HashSet<String>();
            Set<String> oldAliases = new HashSet<String>(filteredPaths);
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
                    if (norm == null) {
                        norm = SimpleXMLSource.normalize(valueToMatch);
                    }
                    String value = getValueAtDPath(alias);
                    if (SimpleXMLSource.normalize(value).equals(norm)) {
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
     * See CLDRFile getWinningPath for documentation.
     * Default implementation is that it removes draft and [@alt="...proposed..." if possible
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

    /**
     * Adds a listener to this XML source.
     */
    public void addListener(Listener listener) {
        listeners.add(new WeakReference<Listener>(listener));
    }

    /**
     * Notifies all listeners that a change has occurred. This method should be
     * called by the XMLSource being updated after any change
     * (usually in putValueAtDPath() and removeValueAtDPath()).
     * This should only be called by XMLSource / CLDRFile
     *
     * @param xpath
     *            the xpath where the change occurred.
     */
    protected void notifyListeners(String xpath) {
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
     * return true if the path in this file (without resolution). Default implementation is to just see if the path has
     * a value.
     * The resolved source must just test the top level.
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
    public abstract void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result);

    public VersionInfo getDtdVersionInfo() {
        return null;
    }

    public String getBaileyValue(String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
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
}
