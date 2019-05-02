/*
 **********************************************************************
 * Copyright (c) 2002-2018, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.With.SimpleIterator;
import org.unicode.cldr.util.XMLSource.ResolvingSource;
import org.unicode.cldr.util.XPathParts.Comments;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * This is a class that represents the contents of a CLDR file, as <key,value> pairs,
 * where the key is a "cleaned" xpath (with non-distinguishing attributes removed),
 * and the value is an object that contains the full
 * xpath plus a value, which is a string, or a node (the latter for atomic elements).
 * <p>
 * <b>WARNING: The API on this class is likely to change.</b> Having the full xpath on the value is clumsy; I need to
 * change it to having the key be an object that contains the full xpath, but then sorts as if it were clean.
 * <p>
 * Each instance also contains a set of associated comments for each xpath.
 *
 * @author medavis
 */

/*
 * Notes:
 * http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
 * http://developers.sun.com/dev/coolstuff/xml/readme.html
 * http://lists.xml.org/archives/xml-dev/200007/msg00284.html
 * http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */

public class CLDRFile implements Freezable<CLDRFile>, Iterable<String> {

    /**
     * Variable to control whether File reads are buffered; this will about halve the time spent in
     * loadFromFile() and Factory.make() from about 20 % to about 10 %. It will also noticeably improve the different
     * unit tests take in the TestAll fixture.
     *  TRUE - use buffering (default)
     *  FALSE - do not use buffering
     */
    private static final boolean USE_LOADING_BUFFER = true;
    private static final boolean WRITE_COMMENTS_THAT_NO_LONGER_HAVE_BASE = false;

    private static final boolean DEBUG = false;

    public static final Pattern ALT_PROPOSED_PATTERN = PatternCache.get(".*\\[@alt=\"[^\"]*proposed[^\"]*\"].*");

    private static boolean LOG_PROGRESS = false;

    public static boolean HACK_ORDER = false;
    private static boolean DEBUG_LOGGING = false;

    public static final String SUPPLEMENTAL_NAME = "supplementalData";
    public static final String SUPPLEMENTAL_METADATA = "supplementalMetadata";
    public static final String SUPPLEMENTAL_PREFIX = "supplemental";
    public static final String GEN_VERSION = "35";
    public static final List<String> SUPPLEMENTAL_NAMES = Arrays.asList("characters", "coverageLevels", "dayPeriods", "genderList", "languageInfo",
        "languageGroup", "likelySubtags", "metaZones", "numberingSystems", "ordinals", "plurals", "postalCodeData", "rgScope", "supplementalData",
        "supplementalMetadata",
        "telephoneCodeData", "windowsZones");

    private Collection<String> extraPaths = null;

    private boolean locked;
    private DtdType dtdType;
    private DtdData dtdData;

    XMLSource dataSource; // TODO(jchye): make private

    private File supplementalDirectory;

    public enum DraftStatus {
        unconfirmed, provisional, contributed, approved;

        public static DraftStatus forString(String string) {
            return string == null ? DraftStatus.approved
                : DraftStatus.valueOf(string.toLowerCase(Locale.ENGLISH));
        }
    };

    public String toString() {
        return "{"
            + "locked=" + locked
            + " locale=" + dataSource.getLocaleID()
            + " dataSource=" + dataSource.toString()
            + "}";
    }

    public String toString(String regex) {
        return "{"
            + "locked=" + locked
            + " locale=" + dataSource.getLocaleID()
            + " regex=" + regex
            + " dataSource=" + dataSource.toString(regex)
            + "}";
    }

    // for refactoring

    public CLDRFile setNonInheriting(boolean isSupplemental) {
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        dataSource.setNonInheriting(isSupplemental);
        return this;
    }

    public boolean isNonInheriting() {
        return dataSource.isNonInheriting();
    }

    /**
     * Construct a new CLDRFile.
     *
     * @param dataSource
     *            must not be null
     */
    public CLDRFile(XMLSource dataSource) {
        this.dataSource = dataSource;
        // source.xpath_value = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
    }

    public CLDRFile(XMLSource dataSource, XMLSource... resolvingParents) {
        List<XMLSource> sourceList = new ArrayList<XMLSource>();
        sourceList.add(dataSource);
        sourceList.addAll(Arrays.asList(resolvingParents));
        this.dataSource = new ResolvingSource(sourceList);
        // source.xpath_value = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
    }

    public static CLDRFile loadFromFile(File f, String localeName, DraftStatus minimalDraftStatus, XMLSource source) {
        String fullFileName = f.getAbsolutePath();
        try {
            fullFileName = f.getCanonicalPath();
            if (DEBUG_LOGGING) {
                System.out.println("Parsing: " + fullFileName);
                Log.logln(LOG_PROGRESS, "Parsing: " + fullFileName);
            }
            final CLDRFile cldrFile;
            if (USE_LOADING_BUFFER) {
                // Use Buffering -  improves performance at little cost to memory footprint
                // try (InputStream fis = new BufferedInputStream(new FileInputStream(f),32000);) {
                try (InputStream fis = InputStreamFactory.createInputStream(f)) {
                    cldrFile = load(fullFileName, localeName, fis, minimalDraftStatus, source);
                    return cldrFile;
                }
            } else {
                // previous version - do not use buffering
                try (InputStream fis = new FileInputStream(f);) {
                    cldrFile = load(fullFileName, localeName, fis, minimalDraftStatus, source);
                    return cldrFile;
                }
            }

        } catch (Exception e) {
            // use a StringBuilder to construct the message.
            StringBuilder sb = new StringBuilder("Cannot read the file '");
            sb.append(fullFileName);
            sb.append("': ");
            sb.append(e.getMessage());
            throw new ICUUncheckedIOException(sb.toString(), e);
        }
    }

    public static CLDRFile loadFromFiles(List<File> dirs, String localeName, DraftStatus minimalDraftStatus, XMLSource source) {
        try {
            if (DEBUG_LOGGING) {
                System.out.println("Parsing: " + dirs);
                Log.logln(LOG_PROGRESS, "Parsing: " + dirs);
            }
            if (USE_LOADING_BUFFER) {
                // Use Buffering -  improves performance at little cost to memory footprint
                // try (InputStream fis = new BufferedInputStream(new FileInputStream(f),32000);) {
                CLDRFile cldrFile = new CLDRFile(source);
                for (File dir : dirs) {
                    File f = new File(dir, localeName + ".xml");
                    try (InputStream fis = InputStreamFactory.createInputStream(f)) {
                        cldrFile.loadFromInputStream(f.getCanonicalPath(), localeName, fis, minimalDraftStatus);
                    }
                }
                return cldrFile;
            } else {
                throw new IllegalArgumentException("Must use USE_LOADING_BUFFER");
            }

        } catch (Exception e) {
            // e.printStackTrace();
            // use a StringBuilder to construct the message.
            StringBuilder sb = new StringBuilder("Cannot read the file '");
            sb.append(dirs);
            throw new ICUUncheckedIOException(sb.toString(), e);
        }
    }

    /**
     * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
     *
     * @param localeName
     * @param dir
     *            directory
     */
    public static CLDRFile loadFromFile(File f, String localeName, DraftStatus minimalDraftStatus) {
        return loadFromFile(f, localeName, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    public static CLDRFile loadFromFiles(List<File> dirs, String localeName, DraftStatus minimalDraftStatus) {
        return loadFromFiles(dirs, localeName, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    static CLDRFile load(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
        return load(fileName, localeName, fis, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    /**
     * Load a CLDRFile from a file input stream.
     *
     * @param localeName
     * @param fis
     */
    private static CLDRFile load(String fileName, String localeName, InputStream fis,
        DraftStatus minimalDraftStatus,
        XMLSource source) {
        CLDRFile cldrFile = new CLDRFile(source);
        return cldrFile.loadFromInputStream(fileName, localeName, fis, minimalDraftStatus);
    }

    /**
     * Low-level function, only normally used for testing.
     * @param fileName
     * @param localeName
     * @param fis
     * @param minimalDraftStatus
     * @return
     */
    public CLDRFile loadFromInputStream(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
        CLDRFile cldrFile = this;
        try {
            fis = new StripUTF8BOMInputStream(fis);
            MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(cldrFile, minimalDraftStatus);

            // now fill it.

            XMLReader xmlReader = createXMLReader(true);
            xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
            xmlReader.setErrorHandler(DEFAULT_DECLHANDLER);
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
            xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
            InputSource is = new InputSource(fis);
            is.setSystemId(fileName);
            xmlReader.parse(is);
            if (DEFAULT_DECLHANDLER.isSupplemental < 0) {
                throw new IllegalArgumentException("root of file must be either ldml or supplementalData");
            }
            cldrFile.setNonInheriting(DEFAULT_DECLHANDLER.isSupplemental > 0);
            if (DEFAULT_DECLHANDLER.overrideCount > 0) {
                throw new IllegalArgumentException("Internal problems: either data file has duplicate path, or" +
                    " CLDRFile.isDistinguishing() or CLDRFile.isOrdered() need updating: "
                    + DEFAULT_DECLHANDLER.overrideCount
                    + "; The exact problems are printed on the console above.");
            }
            if (localeName == null) {
                cldrFile.dataSource.setLocaleID(cldrFile.getLocaleIDFromIdentity());
            }
            return cldrFile;
        } catch (SAXParseException e) {
            // System.out.println(CLDRFile.showSAX(e));
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + localeName + "\t"
                + CLDRFile.showSAX(e)).initCause(e);
        } catch (SAXException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + localeName).initCause(e);
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Can't read " + localeName, e);
        }
    }

    /**
     * Clone the object. Produces unlocked version
     *
     * @see com.ibm.icu.dev.test.util.Freezeble
     */
    public CLDRFile cloneAsThawed() {
        try {
            CLDRFile result = (CLDRFile) super.clone();
            result.locked = false;
            result.dataSource = (XMLSource) result.dataSource.cloneAsThawed();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("should never happen");
        }
    }

    /**
     * Prints the contents of the file (the xpaths/values) to the console.
     *
     */
    public CLDRFile show() {
        for (Iterator<String> it2 = iterator(); it2.hasNext();) {
            String xpath = it2.next();
            System.out.println(getFullXPath(xpath) + " =>\t" + getStringValue(xpath));
        }
        return this;
    }

    private final static Map<String, Object> nullOptions = Collections.unmodifiableMap(new TreeMap<String, Object>());

    /**
     * Write the corresponding XML file out, with the normal formatting and indentation.
     * Will update the identity element, including version, and other items.
     * If the CLDRFile is empty, the DTD type will be //ldml.
     */
    public void write(PrintWriter pw) {
        write(pw, nullOptions);
    }

    /**
     * Write the corresponding XML file out, with the normal formatting and indentation.
     * Will update the identity element, including version, and other items.
     * If the CLDRFile is empty, the DTD type will be //ldml.
     *
     * @param pw
     *            writer to print to
     * @param options
     *            map of options for writing
     * @return true if we write the file, false if we cancel due to skipping all paths
     */
    public boolean write(PrintWriter pw, Map<String, ?> options) {
        Set<String> orderedSet = new TreeSet<String>(getComparator());
        CollectionUtilities.addAll(dataSource.iterator(), orderedSet);

        String firstPath = null;
        String firstFullPath = null;
        XPathParts firstFullPathParts = null;
        DtdType dtdType = DtdType.ldml; // default
        boolean suppressInheritanceMarkers = false;

        if (orderedSet.size() > 0) { // May not have any elements.
            firstPath = (String) orderedSet.iterator().next();
            firstFullPath = getFullXPath(firstPath);
            firstFullPathParts = XPathParts.getTestInstance(firstFullPath);
            dtdType = DtdType.valueOf(firstFullPathParts.getElement(0));
        }

        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        if (!options.containsKey("DTD_OMIT")) {
            // <!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
            // <!DOCTYPE supplementalData SYSTEM '../../common/dtd/ldmlSupplemental.dtd'>
            String fixedPath = "../../" + dtdType.dtdPath;

            if (options.containsKey("DTD_DIR")) {
                String dtdDir = options.get("DTD_DIR").toString();
                fixedPath = dtdDir + dtdType + ".dtd";
            }
            pw.println("<!DOCTYPE " + dtdType + " SYSTEM \"" + fixedPath + "\">");
        }

        if (options.containsKey("COMMENT")) {
            pw.println("<!-- " + options.get("COMMENT") + " -->");
        }
        if (options.containsKey("SUPPRESS_IM")) {
            suppressInheritanceMarkers = true;
        }
        /*
         * <identity>
         * <version number="1.2"/>
         * <language type="en"/>
         */
        // if ldml has any attributes, get them.
        Set<String> identitySet = new TreeSet<String>(getComparator());
        if (!isNonInheriting()) {
            String ldml_identity = "//ldml/identity";
            if (firstFullPath != null) { // if we had a path
                if (firstFullPath.indexOf("/identity") >= 0) {
                    ldml_identity = firstFullPathParts.toString(2);
                } else {
                    ldml_identity = firstFullPathParts.toString(1) + "/identity";
                }
            }

            identitySet.add(ldml_identity + "/version[@number=\"$" + "Revision" + "$\"]");
            LocaleIDParser lip = new LocaleIDParser();
            lip.set(dataSource.getLocaleID());
            identitySet.add(ldml_identity + "/language[@type=\"" + lip.getLanguage() + "\"]");
            if (lip.getScript().length() != 0) {
                identitySet.add(ldml_identity + "/script[@type=\"" + lip.getScript() + "\"]");
            }
            if (lip.getRegion().length() != 0) {
                identitySet.add(ldml_identity + "/territory[@type=\"" + lip.getRegion() + "\"]");
            }
            String[] variants = lip.getVariants();
            for (int i = 0; i < variants.length; ++i) {
                identitySet.add(ldml_identity + "/variant[@type=\"" + variants[i] + "\"]");
            }
        }
        // now do the rest

        String initialComment = fixInitialComment(dataSource.getXpathComments().getInitialComment());
        XPathParts.writeComment(pw, 0, initialComment, true);

        XPathParts.Comments tempComments = (XPathParts.Comments) dataSource.getXpathComments().clone();

        XPathParts last = new XPathParts(defaultSuppressionMap);
        XPathParts current = new XPathParts(defaultSuppressionMap);
        XPathParts lastFiltered = new XPathParts(defaultSuppressionMap);
        XPathParts currentFiltered = new XPathParts(defaultSuppressionMap);

        boolean isResolved = dataSource.isResolving();

        java.util.function.Predicate<String> skipTest = (java.util.function.Predicate<String>) options.get("SKIP_PATH");

        for (Iterator<String> it2 = identitySet.iterator(); it2.hasNext();) {
            String xpath = (String) it2.next();
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            /*
             * TODO: XPathParts.getTestInstance -- how to use with defaultSuppressionMap?
             */
            currentFiltered.set(xpath);
            current.set(xpath);
            current.writeDifference(pw, currentFiltered, last, "", tempComments);
            /*
             * Exchange pairs of parts:
             *     swap current with last
             *     swap currentFiltered with lastFiltered
             * However, current and currentFiltered both get "set" next, so the effect should be the same as
             *     last = current
             *     lastFiltered = currentFiltered
             *     current = getInstance
             *     currentFiltered = getInstance
             *     ??
             */
            XPathParts temp = current;
            current = last;
            last = temp;
            temp = currentFiltered;
            currentFiltered = lastFiltered;
            lastFiltered = temp;
        }

        boolean wroteAtLeastOnePath = false;
        for (String xpath : orderedSet) {
            if (skipTest != null
                && skipTest.test(xpath)) {
                continue;
            }
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            String v = getStringValue(xpath);
            if (suppressInheritanceMarkers && CldrUtility.INHERITANCE_MARKER.equals(v)) {
                continue;
            }
            currentFiltered.set(xpath);
            if (currentFiltered.size() >= 2
                && currentFiltered.getElement(1).equals("identity")) { 
                continue;
            }
            current.set(getFullXPath(xpath));
            current.writeDifference(pw, currentFiltered, last, v, tempComments);
            // exchange pairs of parts
            XPathParts temp = current;
            current = last;
            last = temp;
            temp = currentFiltered;
            currentFiltered = lastFiltered;
            lastFiltered = temp;
            wroteAtLeastOnePath = true;
        }
        if (!wroteAtLeastOnePath && options.containsKey("SKIP_FILE_IF_SKIP_ALL_PATHS")) {
            return false;
        }
        current.clear().writeDifference(pw, null, last, null, tempComments);
        String finalComment = dataSource.getXpathComments().getFinalComment();

        if (WRITE_COMMENTS_THAT_NO_LONGER_HAVE_BASE) {
            // write comments that no longer have a base
            List<String> x = tempComments.extractCommentsWithoutBase();
            if (x.size() != 0) {
                String extras = "Comments without bases" + XPathParts.NEWLINE;
                for (Iterator<String> it = x.iterator(); it.hasNext();) {
                    String key = it.next();
                    // Log.logln("Writing extra comment: " + key);
                    extras += XPathParts.NEWLINE + key;
                }
                finalComment += XPathParts.NEWLINE + extras;
            }            
        }
        XPathParts.writeComment(pw, 0, finalComment, true);
        return true;
    }

    static final Splitter LINE_SPLITTER = Splitter.on('\n');

    private String fixInitialComment(String initialComment) {
        if (initialComment == null || initialComment.isEmpty()) {
            return CldrUtility.getCopyrightString();
        } else {
            StringBuilder sb = new StringBuilder(CldrUtility.getCopyrightString()).append(XPathParts.NEWLINE);
            for (String line : LINE_SPLITTER.split(initialComment)) {
                if (line.contains("Copyright")
                    || line.contains("©")
                    || line.contains("trademark")
                    || line.startsWith("CLDR data files are interpreted")
                    || line.startsWith("For terms of use")) {
                    continue;
                }
                sb.append(XPathParts.NEWLINE).append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Get a string value from an xpath.
     */
    public String getStringValue(String xpath) {
        String result = dataSource.getValueAtPath(xpath);
        if (result == null && dataSource.isResolving()) {
            final String fallbackPath = getFallbackPath(xpath, false);
            if (fallbackPath != null) {
                result = dataSource.getValueAtPath(fallbackPath);
            }
        }
        return result;
    }

    /**
     * Get GeorgeBailey value: that is, what the value would be if it were not directly contained in the file.
     * A non-resolving CLDRFile will always return null.
     */
    public String getBaileyValue(String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        String result = dataSource.getBaileyValue(xpath, pathWhereFound, localeWhereFound);
        if ((result == null || result.equals(CldrUtility.INHERITANCE_MARKER)) && dataSource.isResolving()) {
            final String fallbackPath = getFallbackPath(xpath, false);
            if (fallbackPath != null) {
                result = dataSource.getBaileyValue(fallbackPath, pathWhereFound, localeWhereFound);
            }
        }
        return result;
    }

    static final class SimpleAltPicker implements Transform<String, String> {
        public final String alt;

        public SimpleAltPicker(String alt) {
            this.alt = alt;
        }

        public String transform(@SuppressWarnings("unused") String source) {
            return alt;
        }
    };

    /**
     * Get the constructed GeorgeBailey value: that is, if the item would otherwise be constructed (such as "Chinese (Simplified)") use that.
     * Otherwise return BaileyValue.
     * @parameter pathWhereFound null if constructed.
     */
    public String getConstructedBaileyValue(String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        //ldml/localeDisplayNames/languages/language[@type="zh_Hans"]
        if (xpath.startsWith("//ldml/localeDisplayNames/languages/language[@type=\"") && xpath.contains("_")) {
            XPathParts parts = XPathParts.getTestInstance(xpath);
            String type = parts.getAttributeValue(-1, "type");
            if (type.contains("_")) {
                String alt = parts.getAttributeValue(-1, "alt");
                if (localeWhereFound != null) {
                    localeWhereFound.value = getLocaleID();
                }
                if (pathWhereFound != null) {
                    pathWhereFound.value = null; // TODO make more useful
                }
                if (alt == null) {
                    return getName(type, true);
                } else {
                    return getName(type, true, new SimpleAltPicker(alt));
                }
            }
        }
        return getBaileyValue(xpath, pathWhereFound, localeWhereFound);
    }

    /**
     * Only call if xpath doesn't exist in the current file.
     * <p>
     * For now, just handle counts: see getCountPath Also handle extraPaths
     *
     * @param xpath
     * @param winning
     *            TODO
     * @return
     */
    private String getFallbackPath(String xpath, boolean winning) {
        if (xpath.contains("[@count=")) {
            return getCountPathWithFallback(xpath, Count.other, winning);
        }
        if (getRawExtraPaths().contains(xpath)) {
            return xpath;
        }
        return null;
    }

    /**
     * Get the full path from a distinguished path
     */
    public String getFullXPath(String xpath) {
        if (xpath == null) {
            throw new NullPointerException("Null distinguishing xpath");
        }
        String result = dataSource.getFullPath(xpath);
        if (result == null && dataSource.isResolving()) {
            String fallback = getFallbackPath(xpath, true);
            if (fallback != null) {
                // TODO, add attributes from fallback into main
                result = xpath;
            }
        }
        return result;
    }

    /**
     * Get the last modified date (if available) from a distinguished path.
     * @return date or null if not available.
     */
    public Date getLastModifiedDate(String xpath) {
        return dataSource.getChangeDateAtDPath(xpath);
    }

    /**
     * Find out where the value was found (for resolving locales). Returns code-fallback as the location if nothing is
     * found
     *
     * @param distinguishedXPath
     *            path (must be distinguished!)
     * @param status
     *            the distinguished path where the item was found. Pass in null if you don't care.
     */
    public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
        return getSourceLocaleIdExtended(distinguishedXPath, status, true /* skipInheritanceMarker */);
    }

    /**
     * Find out where the value was found (for resolving locales). Returns code-fallback as the location if nothing is
     * found
     *
     * @param distinguishedXPath
     *            path (must be distinguished!)
     * @param status
     *            the distinguished path where the item was found. Pass in null if you don't care.
     * @param skipInheritanceMarker if true, skip sources in which value is INHERITANCE_MARKER
     * @return the locale id as a string
     */
    public String getSourceLocaleIdExtended(String distinguishedXPath, CLDRFile.Status status, boolean skipInheritanceMarker) {
        String result = dataSource.getSourceLocaleIdExtended(distinguishedXPath, status, skipInheritanceMarker);
        if (result == XMLSource.CODE_FALLBACK_ID && dataSource.isResolving()) {
            final String fallbackPath = getFallbackPath(distinguishedXPath, false);
            if (fallbackPath != null && !fallbackPath.equals(distinguishedXPath)) {
                result = dataSource.getSourceLocaleIdExtended(fallbackPath, status, skipInheritanceMarker);
            }
        }
        return result;
    }

    /**
     * return true if the path in this file (without resolution)
     *
     * @param path
     * @return
     */
    public boolean isHere(String path) {
        return dataSource.isHere(path);
    }

    /**
     * Add a new element to a CLDRFile.
     *
     * @param currentFullXPath
     * @param value
     */
    public CLDRFile add(String currentFullXPath, String value) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        // StringValue v = new StringValue(value, currentFullXPath);
        Log.logln(LOG_PROGRESS, "ADDING: \t" + currentFullXPath + " \t" + value + "\t" + currentFullXPath);
        // xpath = xpath.intern();
        try {
            dataSource.putValueAtPath(currentFullXPath, value);
        } catch (RuntimeException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("failed adding " + currentFullXPath + ",\t"
                + value).initCause(e);
        }
        return this;
    }

    public CLDRFile addComment(String xpath, String comment, Comments.CommentType type) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        // System.out.println("Adding comment: <" + xpath + "> '" + comment + "'");
        Log.logln(LOG_PROGRESS, "ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
        if (xpath == null || xpath.length() == 0) {
            dataSource.getXpathComments().setFinalComment(
                CldrUtility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE,
                    comment));
        } else {
            xpath = getDistinguishingXPath(xpath, null);
            dataSource.getXpathComments().addComment(type, xpath, comment);
        }
        return this;
    }

    // TODO Change into enum, update docs
    static final public int MERGE_KEEP_MINE = 0,
        MERGE_REPLACE_MINE = 1,
        MERGE_ADD_ALTERNATE = 2,
        MERGE_REPLACE_MY_DRAFT = 3;

    /**
     * Merges elements from another CLDR file. Note: when both have the same xpath key,
     * the keepMine determines whether "my" values are kept
     * or the other files values are kept.
     *
     * @param other
     * @param keepMine
     *            if true, keep my values in case of conflict; otherwise keep the other's values.
     */
    public CLDRFile putAll(CLDRFile other, int conflict_resolution) {

        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        if (conflict_resolution == MERGE_KEEP_MINE) {
            dataSource.putAll(other.dataSource, MERGE_KEEP_MINE);
        } else if (conflict_resolution == MERGE_REPLACE_MINE) {
            dataSource.putAll(other.dataSource, MERGE_REPLACE_MINE);
        } else if (conflict_resolution == MERGE_REPLACE_MY_DRAFT) {
            // first find all my alt=..proposed items
            Set<String> hasDraftVersion = new HashSet<String>();
            for (Iterator<String> it = dataSource.iterator(); it.hasNext();) {
                String cpath = it.next();
                String fullpath = getFullXPath(cpath);
                if (fullpath.indexOf("[@draft") >= 0) {
                    hasDraftVersion.add(getNondraftNonaltXPath(cpath)); // strips the alt and the draft
                }
            }
            // only replace draft items!
            // this is either an item with draft in the fullpath
            // or an item with draft and alt in the full path
            for (Iterator<String> it = other.iterator(); it.hasNext();) {
                String cpath = it.next();
                cpath = getNondraftNonaltXPath(cpath);
                String newValue = other.getStringValue(cpath);
                String newFullPath = getNondraftNonaltXPath(other.getFullXPath(cpath));
                // another hack; need to add references back in
                newFullPath = addReferencesIfNeeded(newFullPath, getFullXPath(cpath));

                if (!hasDraftVersion.contains(cpath)) {
                    if (cpath.startsWith("//ldml/identity/")) continue; // skip, since the error msg is not needed.
                    String myVersion = getStringValue(cpath);
                    if (myVersion == null || !newValue.equals(myVersion)) {
                        Log.logln(getLocaleID() + "\tDenied attempt to replace non-draft" + CldrUtility.LINE_SEPARATOR
                            + "\tcurr: [" + cpath + ",\t"
                            + myVersion + "]" + CldrUtility.LINE_SEPARATOR + "\twith: [" + newValue + "]");
                        continue;
                    }
                }
                Log.logln(getLocaleID() + "\tVETTED: [" + newFullPath + ",\t" + newValue + "]");
                dataSource.putValueAtPath(newFullPath, newValue);
            }
        } else if (conflict_resolution == MERGE_ADD_ALTERNATE) {
            for (Iterator<String> it = other.iterator(); it.hasNext();) {
                String key = it.next();
                String otherValue = other.getStringValue(key);
                String myValue = dataSource.getValueAtPath(key);
                if (myValue == null) {
                    dataSource.putValueAtPath(other.getFullXPath(key), otherValue);
                } else if (!(myValue.equals(otherValue)
                    && equalsIgnoringDraft(getFullXPath(key), other.getFullXPath(key)))
                    && !key.startsWith("//ldml/identity")) {
                    for (int i = 0;; ++i) {
                        String prop = "proposed" + (i == 0 ? "" : String.valueOf(i));
                        XPathParts parts = XPathParts.getTestInstance(other.getFullXPath(key)); // can't be frozen
                        String fullPath = parts.addAttribute("alt", prop).toString();
                        String path = getDistinguishingXPath(fullPath, null);
                        if (dataSource.getValueAtPath(path) != null) {
                            continue;
                        }
                        dataSource.putValueAtPath(fullPath, otherValue);
                        break;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Illegal operand: " + conflict_resolution);
        }

        dataSource.getXpathComments().setInitialComment(
            CldrUtility.joinWithSeparation(dataSource.getXpathComments().getInitialComment(),
                XPathParts.NEWLINE,
                other.dataSource.getXpathComments().getInitialComment()));
        dataSource.getXpathComments().setFinalComment(
            CldrUtility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(),
                XPathParts.NEWLINE,
                other.dataSource.getXpathComments().getFinalComment()));
        dataSource.getXpathComments().joinAll(other.dataSource.getXpathComments());
        return this;
    }

    /**
     *
     */
    private String addReferencesIfNeeded(String newFullPath, String fullXPath) {
        if (fullXPath == null || fullXPath.indexOf("[@references=") < 0) {
            return newFullPath;
        }
        XPathParts parts = XPathParts.getTestInstance(fullXPath);
        String accummulatedReferences = null;
        for (int i = 0; i < parts.size(); ++i) {
            Map<String, String> attributes = parts.getAttributes(i);
            String references = attributes.get("references");
            if (references == null) {
                continue;
            }
            if (accummulatedReferences == null) {
                accummulatedReferences = references;
            } else {
                accummulatedReferences += ", " + references;                
            }
        }
        if (accummulatedReferences == null) {
            return newFullPath;
        }
        XPathParts newParts = XPathParts.getTestInstance(newFullPath);
        Map<String, String> attributes = newParts.getAttributes(newParts.size() - 1);
        String references = attributes.get("references");
        if (references == null)
            references = accummulatedReferences;
        else
            references += ", " + accummulatedReferences;
        attributes.put("references", references);
        System.out.println("Changing " + newFullPath + " plus " + fullXPath + " to " + newParts.toString());
        return newParts.toString();
    }

    /**
     * Removes an element from a CLDRFile.
     */
    public CLDRFile remove(String xpath) {
        remove(xpath, false);
        return this;
    }

    /**
     * Removes an element from a CLDRFile.
     */
    public CLDRFile remove(String xpath, boolean butComment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        if (butComment) {
            appendFinalComment(dataSource.getFullPath(xpath) + "::<" + dataSource.getValueAtPath(xpath) + ">");
        }
        dataSource.removeValueAtPath(xpath);
        return this;
    }

    /**
     * Removes all xpaths from a CLDRFile.
     */
    public CLDRFile removeAll(Set<String> xpaths, boolean butComment) {
        if (butComment) appendFinalComment("Illegal attributes removed:");
        for (Iterator<String> it = xpaths.iterator(); it.hasNext();) {
            remove(it.next(), butComment);
        }
        return this;
    }

    /**
     * Code should explicitly include CODE_FALLBACK
     */
    public static final Pattern specialsToKeep = PatternCache.get(
        "/(" +
            "measurementSystemName" +
            "|codePattern" +
            "|calendar\\[\\@type\\=\"[^\"]*\"\\]/(?!dateTimeFormats/appendItems)" + // gregorian
            "|numbers/symbols/(decimal/group)" +
            "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
            "|pattern" +
        ")");

    static public final Pattern specialsToPushFromRoot = PatternCache.get(
        "/(" +
            "calendar\\[\\@type\\=\"gregorian\"\\]/" +
            "(?!fields)" +
            "(?!dateTimeFormats/appendItems)" +
            "(?!.*\\[@type=\"format\"].*\\[@type=\"narrow\"])" +
            "(?!.*\\[@type=\"stand-alone\"].*\\[@type=\"(abbreviated|wide)\"])" +
            "|numbers/symbols/(decimal/group)" +
            "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
        ")");

    private static final boolean MINIMIZE_ALT_PROPOSED = false;

    public interface RetentionTest {
        public enum Retention {
            RETAIN, REMOVE, RETAIN_IF_DIFFERENT
        }

        public Retention getRetention(String path);
    }

    /**
     * Removes all items with same value
     *
     * @param keepIfMatches
     *            TODO
     * @param removedItems
     *            TODO
     * @param keepList
     *            TODO
     */
    public CLDRFile removeDuplicates(CLDRFile other, boolean butComment, RetentionTest keepIfMatches,
        Collection<String> removedItems) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        // Matcher specialPathMatcher = dontRemoveSpecials ? specialsToKeep.matcher("") : null;
        boolean first = true;
        if (removedItems == null) {
            removedItems = new ArrayList<String>();
        } else {
            removedItems.clear();
        }
        Set<String> checked = new HashSet<String>();
        for (Iterator<String> it = iterator(); it.hasNext();) { // see what items we have that the other also has
            String curXpath = it.next();
            boolean logicDuplicate = true;

            if (!checked.contains(curXpath)) {
                // we compare logic Group and only removen when all are duplicate
                Set<String> logicGroups = LogicalGrouping.getPaths(this, curXpath);
                Iterator<String> iter = logicGroups.iterator();
                while (iter.hasNext() && logicDuplicate) {
                    String xpath = iter.next();
                    switch (keepIfMatches.getRetention(xpath)) {
                    case RETAIN:
                        logicDuplicate = false;
                        continue;
                    case RETAIN_IF_DIFFERENT:
                        String currentValue = dataSource.getValueAtPath(xpath);
                        if (currentValue == null) {
                            logicDuplicate = false;
                            continue;
                        }
                        String otherXpath = xpath;
                        String otherValue = other.dataSource.getValueAtPath(otherXpath);
                        if (!currentValue.equals(otherValue)) {
                            if (MINIMIZE_ALT_PROPOSED) {
                                otherXpath = CLDRFile.getNondraftNonaltXPath(xpath);
                                if (otherXpath.equals(xpath)) {
                                    logicDuplicate = false;
                                    continue;
                                }
                                otherValue = other.dataSource.getValueAtPath(otherXpath);
                                if (!currentValue.equals(otherValue)) {
                                    logicDuplicate = false;
                                    continue;
                                }
                            } else {
                                logicDuplicate = false;
                                continue;
                            }
                        }
                        String keepValue = (String) XMLSource.getPathsAllowingDuplicates().get(xpath);
                        if (keepValue != null && keepValue.equals(currentValue)) {
                            logicDuplicate = false;
                            continue;
                        }
                        // we've now established that the values are the same
                        String currentFullXPath = dataSource.getFullPath(xpath);
                        String otherFullXPath = other.dataSource.getFullPath(otherXpath);
                        if (!equalsIgnoringDraft(currentFullXPath, otherFullXPath)) {
                            logicDuplicate = false;
                            continue;
                        }
                        if (DEBUG) {
                            keepIfMatches.getRetention(xpath);
                        }
                        break;
                    case REMOVE:
                        if (DEBUG) {
                            keepIfMatches.getRetention(xpath);
                        }
                        break;
                    }

                }
                if (first) {
                    first = false;
                    if (butComment) appendFinalComment("Duplicates removed:");
                }

                // we can't remove right away, since that disturbs the iterator.
                checked.addAll(logicGroups);
                if (logicDuplicate) {
                    removedItems.addAll(logicGroups);
                }
                // remove(xpath, butComment);
            }
        }
        // now remove them safely
        for (String xpath : removedItems) {
            remove(xpath, butComment);
        }
        return this;
    }

    /**
     * @return Returns the finalComment.
     */
    public String getFinalComment() {
        return dataSource.getXpathComments().getFinalComment();
    }

    /**
     * @return Returns the finalComment.
     */
    public String getInitialComment() {
        return dataSource.getXpathComments().getInitialComment();
    }

    /**
     * @return Returns the xpath_comments. Cloned for safety.
     */
    public XPathParts.Comments getXpath_comments() {
        return (XPathParts.Comments) dataSource.getXpathComments().clone();
    }

    /**
     * @return Returns the locale ID. In the case of a supplemental data file, it is SUPPLEMENTAL_NAME.
     */
    public String getLocaleID() {
        return dataSource.getLocaleID();
    }

    /**
     * @return the Locale ID, as declared in the //ldml/identity element
     */
    public String getLocaleIDFromIdentity() {
        ULocale.Builder lb = new ULocale.Builder();
        for (Iterator<String> i = iterator("//ldml/identity/"); i.hasNext();) {
            XPathParts xpp = XPathParts.getTestInstance(i.next());
            String k = xpp.getElement(-1);
            String v = xpp.getAttributeValue(-1, "type");
            if (k.equals("language")) {
                lb = lb.setLanguage(v);
            } else if (k.equals("script")) {
                lb = lb.setScript(v);
            } else if (k.equals("territory")) {
                lb = lb.setRegion(v);
            } else if (k.equals("variant")) {
                lb = lb.setVariant(v);
            }
        }
        return lb.build().toString(); // TODO: CLDRLocale ?
    }

    /**
     * @see com.ibm.icu.util.Freezable#isFrozen()
     */
    public synchronized boolean isFrozen() {
        return locked;
    }

    /**
     * @see com.ibm.icu.util.Freezable#freeze()
     */
    public synchronized CLDRFile freeze() {
        locked = true;
        dataSource.freeze();
        return this;
    }

    public CLDRFile clearComments() {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.setXpathComments(new XPathParts.Comments());
        return this;
    }

    /**
     * Sets a final comment, replacing everything that was there.
     */
    public CLDRFile setFinalComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().setFinalComment(comment);
        return this;
    }

    /**
     * Adds a comment to the final list of comments.
     */
    public CLDRFile appendFinalComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().setFinalComment(
            CldrUtility
            .joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE, comment));
        return this;
    }

    /**
     * Sets the initial comment, replacing everything that was there.
     */
    public CLDRFile setInitialComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().setInitialComment(comment);
        return this;
    }

    // ========== STATIC UTILITIES ==========

    /**
     * Utility to restrict to files matching a given regular expression. The expression does not contain ".xml".
     * Note that supplementalData is always skipped, and root is always included.
     */
    public static Set<String> getMatchingXMLFiles(File sourceDirs[], Matcher m) {
        Set<String> s = new TreeSet<String>();

        for (File dir : sourceDirs) {
            if (!dir.exists()) {
                throw new IllegalArgumentException("Directory doesn't exist:\t" + dir.getPath());
            }
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Input isn't a file directory:\t" + dir.getPath());
            }
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                String name = files[i].getName();
                if (!name.endsWith(".xml") || name.startsWith(".")) continue;
                // if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
                String locale = name.substring(0, name.length() - 4); // drop .xml
                if (!m.reset(locale).matches()) continue;
                s.add(locale);
            }
        }
        return s;
    }

    public Iterator<String> iterator() {
        return dataSource.iterator();
    }

    public synchronized Iterator<String> iterator(String prefix) {
        return dataSource.iterator(prefix);
    }

    public Iterator<String> iterator(Matcher pathFilter) {
        return dataSource.iterator(pathFilter);
    }

    public Iterator<String> iterator(String prefix, Comparator<String> comparator) {
        Iterator<String> it = (prefix == null || prefix.length() == 0)
            ? dataSource.iterator()
                : dataSource.iterator(prefix);
            if (comparator == null) return it;
            Set<String> orderedSet = new TreeSet<String>(comparator);
            CollectionUtilities.addAll(it, orderedSet);
            return orderedSet.iterator();
    }

    public Iterable<String> fullIterable() {
        return new FullIterable(this);
    }

    public static class FullIterable implements Iterable<String>, SimpleIterator<String> {
        private final CLDRFile file;
        private final Iterator<String> fileIterator;
        private Iterator<String> extraPaths;

        FullIterable(CLDRFile file) {
            this.file = file;
            this.fileIterator = file.iterator();
        }

        @Override
        public Iterator<String> iterator() {
            return With.toIterator(this);
        }

        @Override
        public String next() {
            if (fileIterator.hasNext()) {
                return fileIterator.next();
            }
            if (extraPaths == null) {
                extraPaths = file.getExtraPaths().iterator();
            }
            if (extraPaths.hasNext()) {
                return extraPaths.next();
            }
            return null;
        }
    }

    public static String getDistinguishingXPath(String xpath, String[] normalizedPath) {
        return DistinguishedXPath.getDistinguishingXPath(xpath, normalizedPath);
    }

    private static boolean equalsIgnoringDraft(String path1, String path2) {
        if (path1 == path2) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        // TODO: optimize
        if (path1.indexOf("[@draft=") < 0 && path2.indexOf("[@draft=") < 0) {
            return path1.equals(path2);
        }
        return getNondraftNonaltXPath(path1).equals(getNondraftNonaltXPath(path2));
    }

    static XPathParts nondraftParts = new XPathParts();

    public static String getNondraftNonaltXPath(String xpath) {
        if (xpath.indexOf("draft=\"") < 0 && xpath.indexOf("alt=\"") < 0) {
            return xpath;
        }
        synchronized (nondraftParts) {
            XPathParts parts = XPathParts.getTestInstance(xpath);
            String restore;
            HashSet<String> toRemove = new HashSet<String>();
            for (int i = 0; i < parts.size(); ++i) {
                if (parts.getAttributeCount(i) == 0) {
                    continue;
                }
                Map<String, String> attributes = parts.getAttributes(i);
                toRemove.clear();
                restore = null;
                for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                    String attribute = it.next();
                    if (attribute.equals("draft")) {
                        toRemove.add(attribute);
                    } else if (attribute.equals("alt")) {
                        String value = (String) attributes.get(attribute);
                        int proposedPos = value.indexOf("proposed");
                        if (proposedPos >= 0) {
                            toRemove.add(attribute);
                            if (proposedPos > 0) {
                                restore = value.substring(0, proposedPos - 1); // is of form xxx-proposedyyy
                            }
                        }
                    }
                }
                parts.removeAttributes(i, toRemove);
                if (restore != null) {
                    attributes.put("alt", restore);
                }
            }
            return parts.toString();
        }
    }

    /**
     * Determine if an attribute is a distinguishing attribute.
     *
     * @param elementName
     * @param attribute
     * @return
     */
    public static boolean isDistinguishing(DtdType type, String elementName, String attribute) {
        return DtdData.getInstance(type).isDistinguishing(elementName, attribute);
    }

    /**
     * Utility to create a validating XML reader.
     */
    public static XMLReader createXMLReader(boolean validating) {
        String[] testList = {
            "org.apache.xerces.parsers.SAXParser",
            "org.apache.crimson.parser.XMLReaderImpl",
            "gnu.xml.aelfred2.XmlReader",
            "com.bluecast.xml.Piccolo",
            "oracle.xml.parser.v2.SAXParser",
            ""
        };
        XMLReader result = null;
        for (int i = 0; i < testList.length; ++i) {
            try {
                result = (testList[i].length() != 0)
                    ? XMLReaderFactory.createXMLReader(testList[i])
                        : XMLReaderFactory.createXMLReader();
                    result.setFeature("http://xml.org/sax/features/validation", validating);
                    break;
            } catch (SAXException e1) {
            }
        }
        if (result == null)
            throw new NoClassDefFoundError("No SAX parser is available, or unable to set validation correctly");
        try {
            result.setEntityResolver(new CachingEntityResolver());
        } catch (Throwable e) {
            System.err
            .println("WARNING: Can't set caching entity resolver  -  error "
                + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Return a directory to supplemental data used by this CLDRFile.
     * If the CLDRFile is not normally disk-based, the returned directory may be temporary
     * and not guaranteed to exist past the lifetime of the CLDRFile. The directory
     * should be considered read-only.
     */
    public File getSupplementalDirectory() {
        if (supplementalDirectory == null) {
            // ask CLDRConfig.
            supplementalDirectory = CLDRConfig.getInstance().getSupplementalDataInfo().getDirectory();
        }
        return supplementalDirectory;
    }

    public CLDRFile setSupplementalDirectory(File supplementalDirectory) {
        this.supplementalDirectory = supplementalDirectory;
        return this;
    }

    /**
     * Convenience function to return a list of XML files in the Supplemental directory.
     *
     * @return all files ending in ".xml"
     * @see #getSupplementalDirectory()
     */
    public File[] getSupplementalXMLFiles() {
        return getSupplementalDirectory().listFiles(new FilenameFilter() {
            public boolean accept(@SuppressWarnings("unused") File dir, String name) {
                return name.endsWith(".xml");
            }
        });
    }

    /**
     * Convenience function to return a specific supplemental file
     *
     * @param filename
     *            the file to return
     * @return the file (may not exist)
     * @see #getSupplementalDirectory()
     */
    public File getSupplementalFile(String filename) {
        return new File(getSupplementalDirectory(), filename);
    }

    public static boolean isSupplementalName(String localeName) {
        return SUPPLEMENTAL_NAMES.contains(localeName);
    }

    // static String[] keys = {"calendar", "collation", "currency"};
    //
    // static String[] calendar_keys = {"buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil",
    // "japanese"};
    // static String[] collation_keys = {"phonebook", "traditional", "direct", "pinyin", "stroke", "posix", "big5han",
    // "gb2312han"};

    /*    *//**
     * Value that contains a node. WARNING: this is not done yet, and may change.
     * In particular, we don't want to return a Node, since that is mutable, and makes caching unsafe!!
     */
    /*
     * static public class NodeValue extends Value {
     * private Node nodeValue;
     *//**
     * Creation. WARNING, may change.
     *
     * @param value
     * @param currentFullXPath
     */
    /*
     * public NodeValue(Node value, String currentFullXPath) {
     * super(currentFullXPath);
     * this.nodeValue = value;
     * }
     *//**
     * boilerplate
     */

    /*
     * public boolean hasSameValue(Object other) {
     * if (super.hasSameValue(other)) return false;
     * return nodeValue.equals(((NodeValue)other).nodeValue);
     * }
     *//**
     * boilerplate
     */
    /*
     * public String getStringValue() {
     * return nodeValue.toString();
     * }
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.CLDRFile.Value#changePath(java.lang.String)
     *
     * public Value changePath(String string) {
     * return new NodeValue(nodeValue, string);
     * }
     * }
     */

    private static class MyDeclHandler implements DeclHandler, ContentHandler, LexicalHandler, ErrorHandler {
        private static UnicodeSet whitespace = new UnicodeSet("[:whitespace:]");
        private DraftStatus minimalDraftStatus;
        private static final boolean SHOW_START_END = false;
        private int commentStack;
        private boolean justPopped = false;
        private String lastChars = "";
        // private String currentXPath = "/";
        private String currentFullXPath = "/";
        private String comment = null;
        private Map<String, String> attributeOrder;
        private DtdData dtdData;
        private CLDRFile target;
        private String lastActiveLeafNode;
        private String lastLeafNode;
        private int isSupplemental = -1;
        private int[] orderedCounter = new int[30]; // just make deep enough to handle any CLDR file.
        private String[] orderedString = new String[30]; // just make deep enough to handle any CLDR file.
        private int level = 0;
        private int overrideCount = 0;

        MyDeclHandler(CLDRFile target, DraftStatus minimalDraftStatus) {
            this.target = target;
            this.minimalDraftStatus = minimalDraftStatus;
        }

        private String show(Attributes attributes) {
            if (attributes == null) return "null";
            String result = "";
            for (int i = 0; i < attributes.getLength(); ++i) {
                String attribute = attributes.getQName(i);
                String value = attributes.getValue(i);
                result += "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
            }
            return result;
        }

        private void push(String qName, Attributes attributes) {
            // SHOW_ALL &&
            Log.logln(LOG_PROGRESS, "push\t" + qName + "\t" + show(attributes));
            ++level;
            if (!qName.equals(orderedString[level])) {
                // orderedCounter[level] = 0;
                orderedString[level] = qName;
            }
            if (lastChars.length() != 0) {
                if (whitespace.containsAll(lastChars))
                    lastChars = "";
                else
                    throw new IllegalArgumentException("Must not have mixed content: " + qName + ", "
                        + show(attributes) + ", Content: " + lastChars);
            }
            // currentXPath += "/" + qName;
            currentFullXPath += "/" + qName;
            // if (!isSupplemental) ldmlComparator.addElement(qName);
            if (dtdData.isOrdered(qName)) {
                currentFullXPath += orderingAttribute();
            }
            if (attributes.getLength() > 0) {
                attributeOrder.clear();
                for (int i = 0; i < attributes.getLength(); ++i) {
                    String attribute = attributes.getQName(i);
                    String value = attributes.getValue(i);

                    // if (!isSupplemental) ldmlComparator.addAttribute(attribute); // must do BEFORE put
                    // ldmlComparator.addValue(value);
                    // special fix to remove version
                    // <!ATTLIST version number CDATA #REQUIRED >
                    // <!ATTLIST version cldrVersion CDATA #FIXED "24" >
                    if (attribute.equals("cldrVersion")
                        && (qName.equals("version"))) {
                        ((SimpleXMLSource) target.dataSource).setDtdVersionInfo(VersionInfo.getInstance(value));
                    } else {
                        putAndFixDeprecatedAttribute(qName, attribute, value);
                    }
                }
                for (Iterator<String> it = attributeOrder.keySet().iterator(); it.hasNext();) {
                    String attribute = it.next();
                    String value = attributeOrder.get(attribute);
                    String both = "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
                    currentFullXPath += both;
                    // distinguishing = key, registry, alt, and type (except for the type attribute on the elements
                    // default and mapping).
                    // if (isDistinguishing(qName, attribute)) {
                    // currentXPath += both;
                    // }
                }
            }
            if (comment != null) {
                if (currentFullXPath.equals("//ldml") || currentFullXPath.equals("//supplementalData")) {
                    target.setInitialComment(comment);
                } else {
                    target.addComment(currentFullXPath, comment, XPathParts.Comments.CommentType.PREBLOCK);
                }
                comment = null;
            }
            justPopped = false;
            lastActiveLeafNode = null;
            Log.logln(LOG_PROGRESS, "currentFullXPath\t" + currentFullXPath);
        }

        private String orderingAttribute() {
            return "[@_q=\"" + (orderedCounter[level]++) + "\"]";
        }

        private void putAndFixDeprecatedAttribute(String element, String attribute, String value) {
            if (attribute.equals("draft")) {
                if (value.equals("true"))
                    value = "approved";
                else if (value.equals("false")) value = "unconfirmed";
            } else if (attribute.equals("type")) {
                if (changedTypes.contains(element) && isSupplemental < 1) { // measurementSystem for example did not
                    // change from 'type' to 'choice'.
                    attribute = "choice";
                }
            }
            // else if (element.equals("dateFormatItem")) {
            // if (attribute.equals("id")) {
            // String newValue = dateGenerator.getBaseSkeleton(value);
            // if (!fixedSkeletons.contains(newValue)) {
            // fixedSkeletons.add(newValue);
            // if (!value.equals(newValue)) {
            // System.out.println(value + " => " + newValue);
            // }
            // value = newValue;
            // }
            // }
            // }
            attributeOrder.put(attribute, value);
        }

        //private Set<String> fixedSkeletons = new HashSet();

        //private DateTimePatternGenerator dateGenerator = DateTimePatternGenerator.getEmptyInstance();

        /**
         * Types which changed from 'type' to 'choice', but not in supplemental data.
         */
        private static Set<String> changedTypes = new HashSet<String>(Arrays.asList(new String[] {
            "abbreviationFallback",
            "default", "mapping", "measurementSystem", "preferenceOrdering" }));

        static final Pattern draftPattern = PatternCache.get("\\[@draft=\"([^\"]*)\"\\]");
        Matcher draftMatcher = draftPattern.matcher("");

        /**
         * Adds a parsed XPath to the CLDRFile.
         *
         * @param fullXPath
         * @param value
         */
        private void addPath(String fullXPath, String value) {
            String former = target.getStringValue(fullXPath);
            if (former != null) {
                String formerPath = target.getFullXPath(fullXPath);
                if (!former.equals(value) || !fullXPath.equals(formerPath)) {
                    if (!fullXPath.startsWith("//ldml/identity/version") && !fullXPath.startsWith("//ldml/identity/generation")) {
                        warnOnOverride(former, formerPath);
                    }
                }
            }
            value = trimWhitespaceSpecial(value);
            target.add(fullXPath, value);
        }

        private void pop(String qName) {
            Log.logln(LOG_PROGRESS, "pop\t" + qName);
            --level;

            if (lastChars.length() != 0 || justPopped == false) {
                boolean acceptItem = minimalDraftStatus == DraftStatus.unconfirmed;
                if (!acceptItem) {
                    if (draftMatcher.reset(currentFullXPath).find()) {
                        DraftStatus foundStatus = DraftStatus.valueOf(draftMatcher.group(1));
                        if (minimalDraftStatus.compareTo(foundStatus) <= 0) {
                            // what we found is greater than or equal to our status
                            acceptItem = true;
                        }
                    } else {
                        acceptItem = true; // if not found, then the draft status is approved, so it is always ok
                    }
                }
                if (acceptItem) {
                    // Change any deprecated orientation attributes into values
                    // for backwards compatibility.
                    boolean skipAdd = false;
                    if (currentFullXPath.startsWith("//ldml/layout/orientation")) {
                        XPathParts parts = XPathParts.getTestInstance(currentFullXPath); // can be frozen
                        String value = parts.getAttributeValue(-1, "characters");
                        if (value != null) {
                            addPath("//ldml/layout/orientation/characterOrder", value);
                            skipAdd = true;
                        }
                        value = parts.getAttributeValue(-1, "lines");
                        if (value != null) {
                            addPath("//ldml/layout/orientation/lineOrder", value);
                            skipAdd = true;
                        }
                    }
                    if (!skipAdd) {
                        addPath(currentFullXPath, lastChars);
                    }
                    lastLeafNode = lastActiveLeafNode = currentFullXPath;
                }
                lastChars = "";
            } else {
                Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "pop: zeroing last leafNode: "
                    + lastActiveLeafNode);
                lastActiveLeafNode = null;
                if (comment != null) {
                    target.addComment(lastLeafNode, comment, XPathParts.Comments.CommentType.POSTBLOCK);
                    comment = null;
                }
            }
            // currentXPath = stripAfter(currentXPath, qName);
            currentFullXPath = stripAfter(currentFullXPath, qName);
            justPopped = true;
        }

        static Pattern WHITESPACE_WITH_LF = PatternCache.get("\\s*\\u000a\\s*");
        Matcher whitespaceWithLf = WHITESPACE_WITH_LF.matcher("");
        static final UnicodeSet CONTROLS = new UnicodeSet("[:cc:]");

        /**
         * Trim leading whitespace if there is a linefeed among them, then the same with trailing.
         *
         * @param source
         * @return
         */
        private String trimWhitespaceSpecial(String source) {
            if (DEBUG && CONTROLS.containsSome(source)) {
                System.out.println("*** " + source);
            }
            if (!source.contains("\n")) {
                return source;
            }
            source = whitespaceWithLf.reset(source).replaceAll("\n");
            return source;
        }

        private void warnOnOverride(String former, String formerPath) {
            String distinguishing = CLDRFile.getDistinguishingXPath(formerPath, null);
            System.out.println("\tERROR in " + target.getLocaleID()
            + ";\toverriding old value <" + former + "> at path " + distinguishing +
            "\twith\t<" + lastChars + ">" +
            CldrUtility.LINE_SEPARATOR + "\told fullpath: " + formerPath +
            CldrUtility.LINE_SEPARATOR + "\tnew fullpath: " + currentFullXPath);
            overrideCount += 1;
        }

        private static String stripAfter(String input, String qName) {
            int pos = findLastSlash(input);
            if (qName != null) {
                // assert input.substring(pos+1).startsWith(qName);
                if (!input.substring(pos + 1).startsWith(qName)) {
                    throw new IllegalArgumentException("Internal Error: should never get here.");
                }
            }
            return input.substring(0, pos);
        }

        private static int findLastSlash(String input) {
            int braceStack = 0;
            char inQuote = 0;
            for (int i = input.length() - 1; i >= 0; --i) {
                char ch = input.charAt(i);
                switch (ch) {
                case '\'':
                case '"':
                    if (inQuote == 0) {
                        inQuote = ch;
                    } else if (inQuote == ch) {
                        inQuote = 0; // come out of quote
                    }
                    break;
                case '/':
                    if (inQuote == 0 && braceStack == 0) {
                        return i;
                    }
                    break;
                case '[':
                    if (inQuote == 0) {
                        --braceStack;
                    }
                    break;
                case ']':
                    if (inQuote == 0) {
                        ++braceStack;
                    }
                    break;
                }
            }
            return -1;
        }

        // SAX items we need to catch

        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
                throws SAXException {
            Log.logln(LOG_PROGRESS || SHOW_START_END, "startElement uri\t" + uri
                + "\tlocalName " + localName
                + "\tqName " + qName
                + "\tattributes " + show(attributes));
            try {
                if (isSupplemental < 0) { // set by first element
                    attributeOrder = new TreeMap<String, String>(
                        // HACK for ldmlIcu
                        dtdData.dtdType == DtdType.ldml
                        ? CLDRFile.getAttributeOrdering() : dtdData.getAttributeComparator());
                    isSupplemental = target.dtdType == DtdType.ldml ? 0 : 1;
                }
                push(qName, attributes);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            Log.logln(LOG_PROGRESS || SHOW_START_END, "endElement uri\t" + uri + "\tlocalName " + localName
                + "\tqName " + qName);
            try {
                pop(qName);
            } catch (RuntimeException e) {
                // e.printStackTrace();
                throw e;
            }
        }

        //static final char XML_LINESEPARATOR = (char) 0xA;
        //static final String XML_LINESEPARATOR_STRING = String.valueOf(XML_LINESEPARATOR);

        public void characters(char[] ch, int start, int length)
            throws SAXException {
            try {
                String value = new String(ch, start, length);
                Log.logln(LOG_PROGRESS, "characters:\t" + value);
                // we will strip leading and trailing line separators in another place.
                // if (value.indexOf(XML_LINESEPARATOR) >= 0) {
                // value = value.replace(XML_LINESEPARATOR, '\u0020');
                // }
                lastChars += value;
                justPopped = false;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            Log.logln(LOG_PROGRESS, "startDTD name: " + name
                + ", publicId: " + publicId
                + ", systemId: " + systemId);
            commentStack++;
            target.dtdType = DtdType.valueOf(name);
            target.dtdData = dtdData = DtdData.getInstance(target.dtdType);
        }

        public void endDTD() throws SAXException {
            Log.logln(LOG_PROGRESS, "endDTD");
            commentStack--;
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
            final String string = new String(ch, start, length);
            Log.logln(LOG_PROGRESS, commentStack + " comment " + string);
            try {
                if (commentStack != 0) return;
                String comment0 = trimWhitespaceSpecial(string).trim();
                if (lastActiveLeafNode != null) {
                    target.addComment(lastActiveLeafNode, comment0, XPathParts.Comments.CommentType.LINE);
                } else {
                    comment = (comment == null ? comment0 : comment + XPathParts.NEWLINE + comment0);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (LOG_PROGRESS)
                Log.logln(LOG_PROGRESS,
                    "ignorableWhitespace length: " + length + ": " + Utility.hex(new String(ch, start, length)));
            // if (lastActiveLeafNode != null) {
            for (int i = start; i < start + length; ++i) {
                if (ch[i] == '\n') {
                    Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "\\n: zeroing last leafNode: "
                        + lastActiveLeafNode);
                    lastActiveLeafNode = null;
                    break;
                }
            }
            // }
        }

        public void startDocument() throws SAXException {
            Log.logln(LOG_PROGRESS, "startDocument");
            commentStack = 0; // initialize
        }

        public void endDocument() throws SAXException {
            Log.logln(LOG_PROGRESS, "endDocument");
            try {
                if (comment != null) target.addComment(null, comment, XPathParts.Comments.CommentType.LINE);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        // ==== The following are just for debuggin =====

        public void elementDecl(String name, String model) throws SAXException {
            Log.logln(LOG_PROGRESS, "Attribute\t" + name + "\t" + model);
        }

        public void attributeDecl(String eName, String aName, String type, String mode, String value)
            throws SAXException {
            Log.logln(LOG_PROGRESS, "Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }

        public void internalEntityDecl(String name, String value) throws SAXException {
            Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + value);
        }

        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
            Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void processingInstruction(String target, String data)
            throws SAXException {
            Log.logln(LOG_PROGRESS, "processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity(String name)
            throws SAXException {
            Log.logln(LOG_PROGRESS, "skippedEntity: " + name);
        }

        public void setDocumentLocator(Locator locator) {
            Log.logln(LOG_PROGRESS, "setDocumentLocator Locator " + locator);
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            Log.logln(LOG_PROGRESS, "startPrefixMapping prefix: " + prefix +
                ", uri: " + uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            Log.logln(LOG_PROGRESS, "endPrefixMapping prefix: " + prefix);
        }

        public void startEntity(String name) throws SAXException {
            Log.logln(LOG_PROGRESS, "startEntity name: " + name);
        }

        public void endEntity(String name) throws SAXException {
            Log.logln(LOG_PROGRESS, "endEntity name: " + name);
        }

        public void startCDATA() throws SAXException {
            Log.logln(LOG_PROGRESS, "startCDATA");
        }

        public void endCDATA() throws SAXException {
            Log.logln(LOG_PROGRESS, "endCDATA");
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        public void error(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS || true, "error: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        public void fatalError(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS, "fatalError: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        public void warning(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS, "warning: " + showSAX(exception));
            throw exception;
        }
    }

    /**
     * Show a SAX exception in a readable form.
     */
    public static String showSAX(SAXParseException exception) {
        return exception.getMessage()
            + ";\t SystemID: " + exception.getSystemId()
            + ";\t PublicID: " + exception.getPublicId()
            + ";\t LineNumber: " + exception.getLineNumber()
            + ";\t ColumnNumber: " + exception.getColumnNumber();
    }

    /**
     * Says whether the whole file is draft
     */
    public boolean isDraft() {
        String item = (String) iterator().next();
        return item.startsWith("//ldml[@draft=\"unconfirmed\"]");
    }

    // public Collection keySet(Matcher regexMatcher, Collection output) {
    // if (output == null) output = new ArrayList(0);
    // for (Iterator it = keySet().iterator(); it.hasNext();) {
    // String path = (String)it.next();
    // if (regexMatcher.reset(path).matches()) {
    // output.add(path);
    // }
    // }
    // return output;
    // }

    // public Collection keySet(String regexPattern, Collection output) {
    // return keySet(PatternCache.get(regexPattern).matcher(""), output);
    // }

    /**
     * Gets the type of a given xpath, eg script, territory, ...
     * TODO move to separate class
     *
     * @param xpath
     * @return
     */
    public static int getNameType(String xpath) {
        for (int i = 0; i < NameTable.length; ++i) {
            if (!xpath.startsWith(NameTable[i][0])) continue;
            if (xpath.indexOf(NameTable[i][1], NameTable[i][0].length()) >= 0) return i;
        }
        return -1;
    }

    /**
     * Gets the display name for a type
     */
    public static String getNameTypeName(int index) {
        try {
            return getNameName(index);
        } catch (Exception e) {
            return "Illegal Type Name: " + index;
        }
    }

    public static final int NO_NAME = -1, LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2, VARIANT_NAME = 3,
        CURRENCY_NAME = 4, CURRENCY_SYMBOL = 5,
        TZ_EXEMPLAR = 6, TZ_START = TZ_EXEMPLAR,
        TZ_GENERIC_LONG = 7, TZ_GENERIC_SHORT = 8,
        TZ_STANDARD_LONG = 9, TZ_STANDARD_SHORT = 10,
        TZ_DAYLIGHT_LONG = 11, TZ_DAYLIGHT_SHORT = 12,
        TZ_LIMIT = 13,
        KEY_NAME = 13,
        KEY_TYPE_NAME = 14,
        SUBDIVISION_NAME = 15,
        LIMIT_TYPES = 15;

    private static final String[][] NameTable = {
        { "//ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language" },
        { "//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script" },
        { "//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory" },
        { "//ldml/localeDisplayNames/variants/variant[@type=\"", "\"]", "variant" },
        { "//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency" },
        { "//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity", "exemplar-city" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/generic", "tz-generic-long" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/generic", "tz-generic-short" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/standard", "tz-standard-long" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/standard", "tz-standard-short" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/daylight", "tz-daylight-long" },
        { "//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/daylight", "tz-daylight-short" },
        { "//ldml/localeDisplayNames/keys/key[@type=\"", "\"]", "key" },
        { "//ldml/localeDisplayNames/types/type[@key=\"", "\"][@type=\"", "\"]", "key|type" },
        { "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"", "\"]", "subdivision" },

        /**
         * <long>
         * <generic>Newfoundland Time</generic>
         * <standard>Newfoundland Standard Time</standard>
         * <daylight>Newfoundland Daylight Time</daylight>
         * </long>
         * -
         * <short>
         * <generic>NT</generic>
         * <standard>NST</standard>
         * <daylight>NDT</daylight>
         * </short>
         */
    };

    // private static final String[] TYPE_NAME = {"language", "script", "territory", "variant", "currency",
    // "currency-symbol",
    // "tz-exemplar",
    // "tz-generic-long", "tz-generic-short"};

    public Iterator<String> getAvailableIterator(int type) {
        return iterator(NameTable[type][0]);
    }

    /**
     * @return the key used to access data of a given type
     */
    public static String getKey(int type, String code) {
        switch (type) {
        case VARIANT_NAME:
            code = code.toUpperCase(Locale.ROOT);
            break;
        case KEY_NAME:
            code = fixKeyName(code);
            break;
        case TZ_DAYLIGHT_LONG:
        case TZ_DAYLIGHT_SHORT:
        case TZ_EXEMPLAR:
        case TZ_GENERIC_LONG:
        case TZ_GENERIC_SHORT:
        case TZ_STANDARD_LONG:
        case TZ_STANDARD_SHORT:
            code = getLongTzid(code);
            break;
        }
        String[] nameTableRow = NameTable[type];
        if (code.contains("|")) {
            String[] codes = code.split("\\|");
            return nameTableRow[0] + fixKeyName(codes[0]) + nameTableRow[1] + codes[1] + nameTableRow[2];
        } else {
            return nameTableRow[0] + code + nameTableRow[1];
        }
    }

    static final Relation<R2<String, String>, String> bcp47AliasMap = CLDRConfig.getInstance().getSupplementalDataInfo().getBcp47Aliases();

    public static String getLongTzid(String code) {
        if (!code.contains("/")) {
            Set<String> codes = bcp47AliasMap.get(Row.of("tz", code));
            if (codes != null && !codes.isEmpty()) {
                code = codes.iterator().next();
            }
        }
        return code;
    }

    static final ImmutableMap<String, String> FIX_KEY_NAME;
    static {
        Builder<String, String> temp = ImmutableMap.builder();
        for (String s : Arrays.asList("colAlternate", "colBackwards", "colCaseFirst", "colCaseLevel", "colNormalization", "colNumeric", "colReorder",
            "colStrength")) {
            temp.put(s.toLowerCase(Locale.ROOT), s);
        }
        FIX_KEY_NAME = temp.build();
    }

    private static String fixKeyName(String code) {
        String result = FIX_KEY_NAME.get(code);
        return result == null ? code : result;
    }

    /**
     * @return the code used to access data of a given type from the path. Null if not found.
     */
    public static String getCode(String path) {
        int type = getNameType(path);
        if (type < 0) {
            throw new IllegalArgumentException("Illegal type in path: " + path);
        }
        String[] nameTableRow = NameTable[type];
        int start = nameTableRow[0].length();
        int end = path.indexOf(nameTableRow[1], start);
        return path.substring(start, end);
    }

    public String getName(int type, String code) {
        return getName(type, code, null);
    }

    /**
     * Utility for getting the name, given a code.
     *
     * @param type
     * @param code
     * @param codeToAlt - if not null, is called on the code. If the result is not null, then that is used for an alt value.
     * If the alt path has a value it is used, otherwise the normal one is used. For example, the transform could return "short" for
     * PS or HK or MO, but not US or GB.
     * @return
     */
    public String getName(int type, String code, Transform<String, String> codeToAlt) {
        String path = getKey(type, code);
        String result = null;
        if (codeToAlt != null) {
            String alt = codeToAlt.transform(code);
            if (alt != null) {
                result = getStringValueWithBailey(path + "[@alt=\"" + alt + "\"]");
            }
        }
        if (result == null) {
            result = getStringValueWithBailey(path);
        }
        if (getLocaleID().equals("en")) {
            Status status = new Status();
            String sourceLocale = getSourceLocaleID(path, status);
            if (result == null || !sourceLocale.equals("en")) {
                if (type == LANGUAGE_NAME) {
                    Set<String> set = Iso639Data.getNames(code);
                    if (set != null) {
                        return set.iterator().next();
                    }
                    Map<String, Map<String, String>> map = StandardCodes.getLStreg().get("language");
                    Map<String, String> info = map.get(code);
                    if (info != null) {
                        result = info.get("Description");
                    }
                } else if (type == TERRITORY_NAME) {
                    result = getLstrFallback("region", code);
                } else if (type == SCRIPT_NAME) {
                    result = getLstrFallback("script", code);
                }
            }
        }
        return result;
    }

    static final Pattern CLEAN_DESCRIPTION = Pattern.compile("([^\\(\\[]*)[\\(\\[].*");
    static final Splitter DESCRIPTION_SEP = Splitter.on('▪');

    private String getLstrFallback(String codeType, String code) {
        Map<String, String> info = StandardCodes.getLStreg()
            .get(codeType)
            .get(code);
        if (info != null) {
            String temp = info.get("Description");
            if (!temp.equalsIgnoreCase("Private use")) {
                List<String> temp2 = DESCRIPTION_SEP.splitToList(temp);
                temp = temp2.get(0);
                final Matcher matcher = CLEAN_DESCRIPTION.matcher(temp);
                if (matcher.lookingAt()) {
                    return matcher.group(1).trim();
                }
                return temp;
            }
        }
        return null;
    }

    /**
     * Utility for getting a name, given a type and code.
     */
    public String getName(String type, String code) {
        return getName(typeNameToCode(type), code);
    }

    /**
     * @param type
     * @return
     */
    public static int typeNameToCode(String type) {
        if (type.equalsIgnoreCase("region")) {
            type = "territory";
        }
        for (int i = 0; i < LIMIT_TYPES; ++i) {
            if (type.equalsIgnoreCase(getNameName(i))) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Returns the name of the given bcp47 identifier. Note that extensions must
     * be specified using the old "\@key=type" syntax.
     *
     * @param localeOrTZID
     * @return
     */
    public synchronized String getName(String localeOrTZID) {
        return getName(localeOrTZID, false);
    }

    public synchronized String getName(String localeOrTZID, boolean onlyConstructCompound,
        String localeKeyTypePattern, String localePattern, String localeSeparator) {
        return getName(localeOrTZID, onlyConstructCompound,
            localeKeyTypePattern, localePattern, localeSeparator, null);
    }

    /**
     * Returns the name of the given bcp47 identifier. Note that extensions must
     * be specified using the old "\@key=type" syntax.
     * Only used by ExampleGenerator.
     * @param localeOrTZID the locale or timezone ID
     * @param onlyConstructCompound
     * @param localeKeyTypePattern the pattern used to format key-type pairs
     * @param localePattern the pattern used to format primary/secondary subtags
     * @param localeSeparator the list separator for secondary subtags
     * @return
     */
    public synchronized String getName(String localeOrTZID, boolean onlyConstructCompound,
        String localeKeyTypePattern, String localePattern, String localeSeparator,
        Transform<String, String> altPicker) {

        // Hack for seed
        if (localePattern == null) {
            localePattern = "{0} ({1})";
        }

//        // Hack - support BCP47 ids
//        if (localeOrTZID.contains("-") && !localeOrTZID.contains("@") && !localeOrTZID.contains("_")) {
//            localeOrTZID = ULocale.forLanguageTag(localeOrTZID).toString().replace("__", "_");
//        }

        boolean isCompound = localeOrTZID.contains("_");
        String name = isCompound && onlyConstructCompound ? null : getName(LANGUAGE_NAME, localeOrTZID, altPicker);
        // TODO - handle arbitrary combinations
        if (name != null && !name.contains("_") && !name.contains("-")) {
            name = name.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
            return name;
        }
        LanguageTagParser lparser = new LanguageTagParser().set(localeOrTZID);
        return getName(
            lparser, 
            onlyConstructCompound, 
            altPicker, 
            localeKeyTypePattern, 
            localePattern, 
            localeSeparator);
    }

    public String getName(
        LanguageTagParser lparser, 
        boolean onlyConstructCompound, 
        Transform<String, String> altPicker, 
        String localeKeyTypePattern,
        String localePattern, 
        String localeSeparator
        ) {

        String name;
        String original;

        // we need to check for prefixes, for lang+script or lang+country
        boolean haveScript = false;
        boolean haveRegion = false;
        // try lang+script
        if (onlyConstructCompound) {
            name = getName(LANGUAGE_NAME, original = lparser.getLanguage(), altPicker);
            if (name == null) name = original;
        } else {
            name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT_REGION), altPicker);
            if (name != null) {
                haveScript = haveRegion = true;
            } else {
                name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT), altPicker);
                if (name != null) {
                    haveScript = true;
                } else {
                    name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_REGION), altPicker);
                    if (name != null) {
                        haveRegion = true;
                    } else {
                        name = getName(LANGUAGE_NAME, original = lparser.getLanguage(), altPicker);
                        if (name == null) name = original;
                    }
                }
            }
        }
        name = name.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');

        String extras = "";
        if (!haveScript) {
            extras = addDisplayName(lparser.getScript(), SCRIPT_NAME, localeSeparator, extras, altPicker);
        }
        if (!haveRegion) {
            extras = addDisplayName(lparser.getRegion(), TERRITORY_NAME, localeSeparator, extras, altPicker);
        }
        List<String> variants = lparser.getVariants();
        for (String orig : variants) {
            extras = addDisplayName(orig, VARIANT_NAME, localeSeparator, extras, altPicker);
        }

        // Look for key-type pairs.
        main:
            for (Entry<String, List<String>> extension : lparser.getLocaleExtensionsDetailed().entrySet()) {
                String key = extension.getKey();
                if (key.equals("h0")) {
                    continue;
                }
                List<String> keyValue = extension.getValue();
                String oldFormatType = (key.equals("ca") ? JOIN_HYPHEN : JOIN_UNDERBAR).join(keyValue); // default value
                // Check if key/type pairs exist in the CLDRFile first.
                String value = getKeyValueName(key, oldFormatType);
                if (value != null) {
                    value = value.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                } else {
                    // if we fail, then we construct from the key name and the value
                    String kname = getKeyName(key);
                    if (kname == null) {
                        kname = key; // should not happen, but just in case
                    }
                    switch(key) {
                    case "t":
                        List<String> hybrid = lparser.getLocaleExtensionsDetailed().get("h0");
                        if (hybrid != null) {
                            kname = getKeyValueName("h0", JOIN_UNDERBAR.join(hybrid)); 
                        }
                        oldFormatType = getName(oldFormatType);
                        break;
                    case "h0": 
                        continue main;
                    case "cu":
                        oldFormatType = getName(CURRENCY_SYMBOL, oldFormatType.toUpperCase(Locale.ROOT));
                        break;
                    case "tz":
                        oldFormatType = getTZName(oldFormatType, "VVVV");
                        break;
                    case "kr":
                        oldFormatType = getReorderName(localeSeparator, keyValue);
                        break;
                    case "rg": case "sd":
                        oldFormatType = getName(SUBDIVISION_NAME, oldFormatType);
                        break;
                    default: oldFormatType = JOIN_HYPHEN.join(keyValue);
                    }
                    value = MessageFormat.format(localeKeyTypePattern, new Object[] { kname, oldFormatType });
                    value = value.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
                }
                extras = extras.isEmpty() ? value : MessageFormat.format(localeSeparator, new Object[] { extras, value });
            }
        // now handle stray extensions
        for (Entry<String, List<String>> extension : lparser.getExtensionsDetailed().entrySet()) {
            String value = MessageFormat.format(localeKeyTypePattern, new Object[] { extension.getKey(), JOIN_HYPHEN.join(extension.getValue()) });
            extras = extras.isEmpty() ? value : MessageFormat.format(localeSeparator, new Object[] { extras, value });
        }
        // fix this -- shouldn't be hardcoded!
        if (extras.length() == 0) {
            return name;
        }
        return MessageFormat.format(localePattern, new Object[] { name, extras });
    }

    /**
     * Gets timezone name. Not optimized.
     * @param tzcode
     * @return
     */
    private String getTZName(String tzcode, String format) {
        String longid = getLongTzid(tzcode);
        TimezoneFormatter tzf = new TimezoneFormatter(this);
        return tzf.getFormattedZone(longid, format, 0);
    }

    private String getReorderName(String localeSeparator, List<String> keyValues) {
        String result = null;
        for (String value : keyValues) {
            String name = getName(SCRIPT_NAME, Character.toUpperCase(value.charAt(0)) + value.substring(1));
            if (name == null) {
                name = getKeyValueName("kr", value);
                if (name == null) {
                    name = value;
                }
            }
            result = result == null ? name : MessageFormat.format(localeSeparator, new Object[] { result, name });
        }
        return result;
    }

    static final Joiner JOIN_HYPHEN = Joiner.on('-');
    static final Joiner JOIN_UNDERBAR = Joiner.on('_');

    public String getName(LanguageTagParser lparser, 
        boolean onlyConstructCompound, 
        Transform<String, String> altPicker) { 
        return getName(lparser, onlyConstructCompound, altPicker, 
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern"),
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localePattern"),
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator"));
    }

    public String getKeyName(String key) {
        String result = getStringValue("//ldml/localeDisplayNames/keys/key[@type=\"" + key + "\"]");
        if (result == null) {
            Relation<R2<String, String>, String> toAliases = SupplementalDataInfo.getInstance().getBcp47Aliases();
            Set<String> aliases = toAliases.get(Row.of(key, ""));
            if (aliases != null) {
                for (String alias : aliases) {
                    result = getStringValue("//ldml/localeDisplayNames/keys/key[@type=\"" + alias + "\"]");
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public String getKeyValueName(String key, String value) {
        String result = getStringValue("//ldml/localeDisplayNames/types/type[@key=\"" + key + "\"][@type=\"" + value + "\"]");
        if (result == null) {
            Relation<R2<String, String>, String> toAliases = SupplementalDataInfo.getInstance().getBcp47Aliases();
            Set<String> keyAliases = toAliases.get(Row.of(key, ""));
            Set<String> valueAliases = toAliases.get(Row.of(key, value));
            if (keyAliases != null || valueAliases != null) {
                if (keyAliases == null) {
                    keyAliases = Collections.singleton(key);
                }
                if (valueAliases == null) {
                    valueAliases = Collections.singleton(value);
                }
                for (String keyAlias : keyAliases) {
                    for (String valueAlias : valueAliases) {
                        result = getStringValue("//ldml/localeDisplayNames/types/type[@key=\"" + keyAlias + "\"][@type=\"" + valueAlias + "\"]");
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }



    /**
     * Returns the name of the given bcp47 identifier. Note that extensions must
     * be specified using the old "\@key=type" syntax.
     * @param localeOrTZID the locale or timezone ID
     * @param onlyConstructCompound
     * @return
     */
    public synchronized String getName(String localeOrTZID, boolean onlyConstructCompound) {
        return getName(localeOrTZID, onlyConstructCompound, null);
    }

    /**
     * For use in getting short names.
     */
    public static final Transform<String, String> SHORT_ALTS = new Transform<String, String>() {
        public String transform(@SuppressWarnings("unused") String source) {
            return "short";
        }
    };

    /**
     * Returns the name of the given bcp47 identifier. Note that extensions must
     * be specified using the old "\@key=type" syntax.
     * @param localeOrTZID the locale or timezone ID
     * @param onlyConstructCompound if true, returns "English (United Kingdom)" instead of "British English"
     * @param altPicker Used to select particular alts. For example, SHORT_ALTS can be used to get "English (U.K.)"
     * instead of "English (United Kingdom)"
     * @return
     */
    public synchronized String getName(String localeOrTZID,
        boolean onlyConstructCompound,
        Transform<String, String> altPicker) {
        return getName(localeOrTZID, 
            onlyConstructCompound,
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern"),
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localePattern"),
            getWinningValueWithBailey("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator"),
            altPicker
            );
    }

    /**
     * Adds the display name for a subtag to a string.
     * @param subtag the subtag
     * @param type the type of the subtag
     * @param separatorPattern the pattern to be used for separating display
     *      names in the resultant string
     * @param extras the string to be added to
     * @return the modified display name string
     */
    private String addDisplayName(String subtag, int type, String separatorPattern, String extras,
        Transform<String, String> altPicker) {
        if (subtag.length() == 0) return extras;

        String sname = getName(type, subtag, altPicker);
        if (sname == null) {
            sname = subtag;
        }
        sname = sname.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');

        if (extras.length() == 0) {
            extras += sname;
        } else {
            extras = MessageFormat.format(separatorPattern, new Object[] { extras, sname });
        }
        return extras;
    }

    /**
     * Returns the name of a type.
     */
    public static String getNameName(int choice) {
        String[] nameTableRow = NameTable[choice];
        return nameTableRow[nameTableRow.length - 1];
    }

    /**
     * Get standard ordering for elements.
     *
     * @return ordered collection with items.
     * @deprecated
     */
    public static List<String> getElementOrder() {
        return Collections.emptyList(); // elementOrdering.getOrder(); // already unmodifiable
    }

    /**
     * Get standard ordering for attributes.
     *
     * @return ordered collection with items.
     */
    public static List<String> getAttributeOrder() {
        return getAttributeOrdering().getOrder(); // already unmodifiable
    }

    public static boolean isOrdered(String element, DtdType type) {
        return DtdData.getInstance(type).isOrdered(element);
    }

    private static Comparator<String> ldmlComparator = DtdData.getInstance(DtdType.ldmlICU).getDtdComparator(null);

    private final static Map<String, Map<String, String>> defaultSuppressionMap;
    static {
        String[][] data = {
            { "ldml", "version", GEN_VERSION },
            { "version", "cldrVersion", "*" },
            { "orientation", "characters", "left-to-right" },
            { "orientation", "lines", "top-to-bottom" },
            { "weekendStart", "time", "00:00" },
            { "weekendEnd", "time", "24:00" },
            { "dateFormat", "type", "standard" },
            { "timeFormat", "type", "standard" },
            { "dateTimeFormat", "type", "standard" },
            { "decimalFormat", "type", "standard" },
            { "scientificFormat", "type", "standard" },
            { "percentFormat", "type", "standard" },
            { "pattern", "type", "standard" },
            { "currency", "type", "standard" },
            { "transform", "visibility", "external" },
            { "*", "_q", "*" },
        };
        Map<String, Map<String, String>> tempmain = asMap(data, true);
        defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
    }

    public static Map<String, Map<String, String>> getDefaultSuppressionMap() {
        return defaultSuppressionMap;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map asMap(String[][] data, boolean tree) {
        Map tempmain = tree ? (Map) new TreeMap() : new HashMap();
        int len = data[0].length; // must be same for all elements
        for (int i = 0; i < data.length; ++i) {
            Map temp = tempmain;
            if (len != data[i].length) {
                throw new IllegalArgumentException("Must be square array: fails row " + i);
            }
            for (int j = 0; j < len - 2; ++j) {
                Map newTemp = (Map) temp.get(data[i][j]);
                if (newTemp == null) {
                    temp.put(data[i][j], newTemp = tree ? (Map) new TreeMap() : new HashMap());
                }
                temp = newTemp;
            }
            temp.put(data[i][len - 2], data[i][len - 1]);
        }
        return tempmain;
    }

    /**
     * Removes a comment.
     */
    public CLDRFile removeComment(String string) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().removeComment(string);
        return this;
    }

    /**
     * @param draftStatus
     *            TODO
     *
     */
    public CLDRFile makeDraft(DraftStatus draftStatus) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        for (Iterator<String> it = dataSource.iterator(); it.hasNext();) {
            String path = (String) it.next();
            XPathParts parts = XPathParts.getTestInstance(dataSource.getFullPath(path)); // not frozen
            parts.addAttribute("draft", draftStatus.toString());
            dataSource.putValueAtPath(parts.toString(), dataSource.getValueAtPath(path));
        }
        return this;
    }

    public UnicodeSet getExemplarSet(String type, WinningChoice winningChoice) {
        return getExemplarSet(type, winningChoice, UnicodeSet.CASE);
    }

    public UnicodeSet getExemplarSet(ExemplarType type, WinningChoice winningChoice) {
        return getExemplarSet(type, winningChoice, UnicodeSet.CASE);
    }

    static final UnicodeSet HACK_CASE_CLOSURE_SET = new UnicodeSet(
        "[ſẛﬀẞ{i̇}\u1F71\u1F73\u1F75\u1F77\u1F79\u1F7B\u1F7D\u1FBB\u1FBE\u1FC9\u1FCB\u1FD3\u1FDB\u1FE3\u1FEB\u1FF9\u1FFB\u2126\u212A\u212B]")
        .freeze();

    public enum ExemplarType {
        main, auxiliary, index, punctuation, numbers;

        public static ExemplarType fromString(String type) {
            return type.isEmpty() ? main : valueOf(type);
        }
    }

    public UnicodeSet getExemplarSet(String type, WinningChoice winningChoice, int option) {
        return getExemplarSet(ExemplarType.fromString(type), winningChoice, option);
    }

    public UnicodeSet getExemplarSet(ExemplarType type, WinningChoice winningChoice, int option) {
        String path = getExemplarPath(type);
        if (winningChoice == WinningChoice.WINNING) {
            path = getWinningPath(path);
        }
        String v = getStringValueWithBailey(path);
        if (v == null) {
            return UnicodeSet.EMPTY;
        }
        UnicodeSet result = new UnicodeSet(v);
        UnicodeSet toNuke = new UnicodeSet(HACK_CASE_CLOSURE_SET).removeAll(result);
        result.closeOver(UnicodeSet.CASE);
        result.removeAll(toNuke);
        result.remove(0x20);
        return result;
    }

    public static String getExemplarPath(ExemplarType type) {
        return "//ldml/characters/exemplarCharacters" + (type == ExemplarType.main ? "" : "[@type=\"" + type + "\"]");
    }

    public enum NumberingSystem {
        latin(null), defaultSystem("//ldml/numbers/defaultNumberingSystem"), nativeSystem("//ldml/numbers/otherNumberingSystems/native"), traditional(
            "//ldml/numbers/otherNumberingSystems/traditional"), finance("//ldml/numbers/otherNumberingSystems/finance");
        public final String path;

        private NumberingSystem(String path) {
            this.path = path;
        }
    };

    public UnicodeSet getExemplarsNumeric(NumberingSystem system) {
        String numberingSystem = system.path == null ? "latn" : getStringValue(system.path);
        if (numberingSystem == null) {
            return UnicodeSet.EMPTY;
        }
        return getExemplarsNumeric(numberingSystem);
    }

    public UnicodeSet getExemplarsNumeric(String numberingSystem) {
        UnicodeSet result = new UnicodeSet();
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        String[] symbolPaths = {
            "decimal",
            "group",
            "percentSign",
            "perMille",
            "plusSign",
            "minusSign",
            //"infinity"
        };

        String digits = sdi.getDigits(numberingSystem);
        if (digits != null) { // TODO, get other characters, see ticket:8316
            result.addAll(digits);
        }
        for (String path : symbolPaths) {
            String fullPath = "//ldml/numbers/symbols[@numberSystem=\"" + numberingSystem + "\"]/" + path;
            String value = getStringValue(fullPath);
            if (value != null) {
                result.add(value);
            }
        }

        return result;
    }

    public String getCurrentMetazone(String zone) {
        for (Iterator<String> it2 = iterator(); it2.hasNext();) {
            String xpath = (String) it2.next();
            if (xpath.startsWith("//ldml/dates/timeZoneNames/zone[@type=\"" + zone + "\"]/usesMetazone")) {
                XPathParts parts = XPathParts.getTestInstance(xpath);
                if (!parts.containsAttribute("to")) {
                    return parts.getAttributeValue(4, "mzone");
                }
            }
        }
        return null;
    }

    public boolean isResolved() {
        return dataSource.isResolving();
    }

    // WARNING: this must go AFTER attributeOrdering is set; otherwise it uses a null comparator!!
    /*
     * TODO: clarify the warning. There is nothing named "attributeOrdering" in this file.
     * This member distinguishedXPath is accessed only by the function getNonDistinguishingAttributes.
     */
    private static final DistinguishedXPath distinguishedXPath = new DistinguishedXPath();

    public static final String distinguishedXPathStats() {
        return DistinguishedXPath.stats();
    }

    private static class DistinguishedXPath {

        public static final String stats() {
            return "distinguishingMap:" + distinguishingMap.size() + " " +
                "normalizedPathMap:" + normalizedPathMap.size();
        }

        private static Map<String, String> distinguishingMap = new ConcurrentHashMap<String, String>();
        private static Map<String, String> normalizedPathMap = new ConcurrentHashMap<String, String>();

        static {
            distinguishingMap.put("", ""); // seed this to make the code simpler
        }

        public static String getDistinguishingXPath(String xpath, String[] normalizedPath) {
            //     synchronized (distinguishingMap) {
            String result = (String) distinguishingMap.get(xpath);
            if (result == null) {
                /*
                 * TODO: fix null getDtdData here if use XPathParts.getInstance instead of XPathParts.set;
                 * getInstance calls cloneAsThawed which makes dtdData null even when this.dtdData was non-null.
                 * Reference: https://unicode.org/cldr/trac/ticket/12007
                 */
                XPathParts distinguishingParts = new XPathParts().set(xpath);
                // XPathParts distinguishingParts = XPathParts.getTestInstance(xpath);

                DtdType type = distinguishingParts.getDtdData().dtdType;
                Set<String> toRemove = new HashSet<String>();

                // first clean up draft and alt
                String draft = null;
                String alt = null;
                String references = "";
                // note: we only need to clean up items that are NOT on the last element,
                // so we go up to size() - 1.

                // note: each successive item overrides the previous one. That's intended

                for (int i = 0; i < distinguishingParts.size() - 1; ++i) {
                    if (distinguishingParts.getAttributeCount(i) == 0) {
                        continue;
                    }
                    toRemove.clear();
                    Map<String, String> attributes = distinguishingParts.getAttributes(i);
                    for (String attribute : attributes.keySet()) {
                        if (attribute.equals("draft")) {
                            draft = (String) attributes.get(attribute);
                            toRemove.add(attribute);
                        } else if (attribute.equals("alt")) {
                            alt = (String) attributes.get(attribute);
                            toRemove.add(attribute);
                        } else if (attribute.equals("references")) {
                            if (references.length() != 0) references += " ";
                            references += (String) attributes.get("references");
                            toRemove.add(attribute);
                        }
                    }
                    distinguishingParts.removeAttributes(i, toRemove);
                }
                if (draft != null || alt != null || references.length() != 0) {
                    // get the last element that is not ordered.
                    int placementIndex = distinguishingParts.size() - 1;
                    while (true) {
                        String element = distinguishingParts.getElement(placementIndex);
                        if (!DtdData.getInstance(type).isOrdered(element)) break;
                        --placementIndex;
                    }
                    if (draft != null) {
                        distinguishingParts.putAttributeValue(placementIndex, "draft", draft);
                    }
                    if (alt != null) {
                        distinguishingParts.putAttributeValue(placementIndex, "alt", alt);
                    }
                    if (references.length() != 0) {
                        distinguishingParts.putAttributeValue(placementIndex, "references", references);
                    }
                    String newXPath = distinguishingParts.toString();
                    if (!newXPath.equals(xpath)) {
                        normalizedPathMap.put(xpath, newXPath); // store differences
                    }
                }

                // now remove non-distinguishing attributes (if non-inheriting)
                for (int i = 0; i < distinguishingParts.size(); ++i) {
                    if (distinguishingParts.getAttributeCount(i) == 0) {
                        continue;
                    }
                    String element = distinguishingParts.getElement(i);
                    toRemove.clear();
                    for (String attribute : distinguishingParts.getAttributeKeys(i)) {
                        if (!isDistinguishing(type, element, attribute)) {
                            toRemove.add(attribute);
                        }
                    }
                    distinguishingParts.removeAttributes(i, toRemove);
                }

                result = distinguishingParts.toString();
                if (result.equals(xpath)) { // don't save the copy if we don't have to.
                    result = xpath;
                }
                distinguishingMap.put(xpath, result);
            }
            if (normalizedPath != null) {
                normalizedPath[0] = (String) normalizedPathMap.get(xpath);
                if (normalizedPath[0] == null) {
                    normalizedPath[0] = xpath;
                }
            }
            return result;
        }

        public Map<String, String> getNonDistinguishingAttributes(String fullPath, Map<String, String> result,
            Set<String> skipList) {
            if (result == null) {
                result = new LinkedHashMap<String, String>();
            } else {
                result.clear();
            }
            XPathParts distinguishingParts = XPathParts.getTestInstance(fullPath);
            DtdType type = distinguishingParts.getDtdData().dtdType;
            for (int i = 0; i < distinguishingParts.size(); ++i) {
                String element = distinguishingParts.getElement(i);
                Map<String, String> attributes = distinguishingParts.getAttributes(i);
                for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                    String attribute = it.next();
                    if (!isDistinguishing(type, element, attribute) && !skipList.contains(attribute)) {
                        result.put(attribute, attributes.get(attribute));
                    }
                }
            }
            return result;
        }
    }

    public static class Status {
        public String pathWhereFound;

        public String toString() {
            return pathWhereFound;
        }
    }

    public static boolean isLOG_PROGRESS() {
        return LOG_PROGRESS;
    }

    public static void setLOG_PROGRESS(boolean log_progress) {
        LOG_PROGRESS = log_progress;
    }

    public boolean isEmpty() {
        return !dataSource.iterator().hasNext();
    }

    public Map<String, String> getNonDistinguishingAttributes(String fullPath, Map<String, String> result,
        Set<String> skipList) {
        return distinguishedXPath.getNonDistinguishingAttributes(fullPath, result, skipList);
    }

    public String getDtdVersion() {
        return dataSource.getDtdVersionInfo().toString();
    }

    public VersionInfo getDtdVersionInfo() {
        VersionInfo result = dataSource.getDtdVersionInfo();
        if (result != null || isEmpty()) {
            return result;
        }
        // for old files, pick the version from the @version attribute
        String path = dataSource.iterator().next();
        String full = getFullXPath(path);
        XPathParts parts = XPathParts.getFrozenInstance(full);
        String versionString = parts.findFirstAttributeValue("version");
        return versionString == null 
            ? null 
                : VersionInfo.getInstance(versionString);
    }

    private boolean contains(Map<String, String> a, Map<String, String> b) {
        for (Iterator<String> it = b.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String otherValue = a.get(key);
            if (otherValue == null) {
                return false;
            }
            String value = b.get(key);
            if (!otherValue.equals(value)) {
                return false;
            }
        }
        return true;
    }

    public String getFullXPath(String path, boolean ignoreOtherLeafAttributes) {
        String result = getFullXPath(path);
        if (result != null) return result;
        XPathParts parts = XPathParts.getTestInstance(path);
        Map<String, String> lastAttributes = parts.getAttributes(parts.size() - 1);
        String base = parts.toString(parts.size() - 1) + "/" + parts.getElement(parts.size() - 1); // trim final element
        for (Iterator<String> it = iterator(base); it.hasNext();) {
            String otherPath = (String) it.next();
            XPathParts other = XPathParts.getTestInstance(otherPath);
            if (other.size() != parts.size()) {
                continue;
            }
            Map<String, String> lastOtherAttributes = other.getAttributes(other.size() - 1);
            if (!contains(lastOtherAttributes, lastAttributes)) {
                continue;
            }
            if (result == null) {
                result = getFullXPath(otherPath);
            } else {
                throw new IllegalArgumentException("Multiple values for path: " + path);
            }
        }
        return result;
    }

    /**
     * Return true if this item is the "winner" in the survey tool
     *
     * @param path
     * @return
     */
    public boolean isWinningPath(String path) {
        return dataSource.isWinningPath(path);
    }

    /**
     * Returns the "winning" path, for use in the survey tool tests, out of all
     * those paths that only differ by having "alt proposed". The exact meaning
     * may be tweaked over time, but the user's choice (vote) has precedence, then
     * any undisputed choice, then the "best" choice of the remainders. A value is
     * always returned if there is a valid path, and the returned value is always
     * a valid path <i>in the resolved file</i>; that is, it may be valid in the
     * parent, or valid because of aliasing.
     *
     * @param path
     * @return path, perhaps with an alt proposed added.
     */
    public String getWinningPath(String path) {
        return dataSource.getWinningPath(path);
    }

    /**
     * Shortcut for getting the string value for the winning path
     *
     * @param path
     * @return
     */
    public String getWinningValue(String path) {
        final String winningPath = getWinningPath(path);
        return winningPath == null ? null : getStringValue(winningPath);
    }

    /**
     * Shortcut for getting the string value for the winning path.
     * If the winning value is an INHERITANCE_MARKER (used in survey
     * tool), then the Bailey value is returned.
     *
     * @param path
     * @return the winning value
     * 
     * TODO: check whether this is called only when appropriate, see https://unicode.org/cldr/trac/ticket/11299
     * Compare getStringValueWithBailey which is identical except getStringValue versus getWinningValue. 
     */
    public String getWinningValueWithBailey(String path) {
        String winningValue = getWinningValue(path);
        if (CldrUtility.INHERITANCE_MARKER.equals(winningValue)) {
            Output<String> localeWhereFound = new Output<String>();
            Output<String> pathWhereFound = new Output<String>();
            winningValue = getBaileyValue(path, pathWhereFound, localeWhereFound);
        }
        return winningValue;
    }

    /**
     * Shortcut for getting the string value for a path.
     * If the string value is an INHERITANCE_MARKER (used in survey
     * tool), then the Bailey value is returned.
     *
     * @param path
     * @return the string value
     * 
     * TODO: check whether this is called only when appropriate, see https://unicode.org/cldr/trac/ticket/11299
     * Compare getWinningValueWithBailey wich is identical except getWinningValue versus getStringValue. 
     */
    public String getStringValueWithBailey(String path) {
        String value = getStringValue(path);
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            Output<String> localeWhereFound = new Output<String>();
            Output<String> pathWhereFound = new Output<String>();
            value = getBaileyValue(path, pathWhereFound, localeWhereFound);
        }
        return value;
    }

    /**
     * Return the distinguished paths that have the specified value. The pathPrefix and pathMatcher
     * can be used to restrict the returned paths to those matching.
     * The pathMatcher can be null (equals .*).
     *
     * @param valueToMatch
     * @param pathPrefix
     * @return
     */
    public Set<String> getPathsWithValue(String valueToMatch, String pathPrefix, Matcher pathMatcher, Set<String> result) {
        if (result == null) {
            result = new HashSet<String>();
        }
        dataSource.getPathsWithValue(valueToMatch, pathPrefix, result);
        if (pathMatcher == null) {
            return result;
        }
        for (Iterator<String> it = result.iterator(); it.hasNext();) {
            String path = it.next();
            if (!pathMatcher.reset(path).matches()) {
                it.remove();
            }
        }
        return result;
    }

    /**
     * Return the distinguished paths that match the pathPrefix and pathMatcher
     * The pathMatcher can be null (equals .*).
     *
     * @param valueToMatch
     * @param pathPrefix
     * @return
     */
    public Set<String> getPaths(String pathPrefix, Matcher pathMatcher, Set<String> result) {
        if (result == null) {
            result = new HashSet<String>();
        }
        for (Iterator<String> it = dataSource.iterator(pathPrefix); it.hasNext();) {
            String path = it.next();
            if (pathMatcher != null && !pathMatcher.reset(path).matches()) {
                continue;
            }
            result.add(path);
        }
        return result;
    }

    public enum WinningChoice {
        NORMAL, WINNING
    };

    /**
     * Used in TestUser to get the "winning" path. Simple implementation just for testing.
     *
     * @author markdavis
     *
     */
    static class WinningComparator implements Comparator<String> {
        String user;

        public WinningComparator(String user) {
            this.user = user;
        }

        /**
         * if it contains the user, sort first. Otherwise use normal string sorting. A better implementation would look
         * at
         * the number of votes next, and whither there was an approved or provisional path.
         */
        public int compare(String o1, String o2) {
            if (o1.contains(user)) {
                if (!o2.contains(user)) {
                    return -1; // if it contains user
                }
            } else if (o2.contains(user)) {
                return 1; // if it contains user
            }
            return o1.compareTo(o2);
        }
    }

    /**
     * This is a test class used to simulate what the survey tool would do.
     *
     * @author markdavis
     *
     */
    public static class TestUser extends CLDRFile {

        Map<String, String> userOverrides = new HashMap<String, String>();

        public TestUser(CLDRFile baseFile, String user, boolean resolved) {
            super(resolved ? baseFile.dataSource : baseFile.dataSource.getUnresolving());
            if (!baseFile.isResolved()) {
                throw new IllegalArgumentException("baseFile must be resolved");
            }
            Relation<String, String> pathMap = Relation.of(new HashMap<String, Set<String>>(), TreeSet.class,
                new WinningComparator(user));
            for (String path : baseFile) {
                String newPath = getNondraftNonaltXPath(path);
                pathMap.put(newPath, path);
            }
            // now reduce the storage by just getting the winning ones
            // so map everything but the first path to the first path
            for (String path : pathMap.keySet()) {
                String winner = null;
                for (String rowPath : pathMap.getAll(path)) {
                    if (winner == null) {
                        winner = rowPath;
                        continue;
                    }
                    userOverrides.put(rowPath, winner);
                }
            }
        }

        @Override
        public String getWinningPath(String path) {
            String trial = userOverrides.get(path);
            if (trial != null) {
                return trial;
            }
            return path;
        }
    }

    /**
     * Returns the extra paths, skipping those that are already represented in the locale.
     *
     * @return
     */
    public Collection<String> getExtraPaths() {
        Set<String> toAddTo = new HashSet<String>();

        // reverse the order because we're hitting some strange behavior

        toAddTo.addAll(getRawExtraPaths());
        for (String path : this) {
            toAddTo.remove(path);
        }

        //        showStars(getLocaleID() + " getExtraPaths", toAddTo);
        //        for (String path : getRawExtraPaths()) {
        //            // don't use getStringValue, since it recurses.
        //            if (!dataSource.hasValueAtDPath(path)) {
        //                toAddTo.add(path);
        //            } else {
        //                if (path.contains("compoundUnit")) {
        //                    for (String path2 : this) {
        //                        if (path2.equals(path)) {
        //                            System.out.println("\t\t" + path);
        //                        }
        //                    }
        //                    System.out.println();
        //                }
        //            }
        //
        //        }
        //        showStars(getLocaleID() + " getExtraPaths", toAddTo);
        return toAddTo;
    }

    /**
     * Returns the extra paths, skipping those that are already represented in the locale.
     *
     * @return
     */
    public Collection<String> getExtraPaths(String prefix, Collection<String> toAddTo) {
        for (String item : getRawExtraPaths()) {
            if (item.startsWith(prefix) && dataSource.getValueAtPath(item) == null) { // don't use getStringValue, since
                // it recurses.
                toAddTo.add(item);
            }
        }
        return toAddTo;
    }

    // extraPaths contains the raw extra paths.
    // It requires filtering in those cases where we don't want duplicate paths.
    /**
     * Returns the raw extra paths, irrespective of what paths are already represented in the locale.
     *
     * @return
     */
    public Collection<String> getRawExtraPaths() {
        if (extraPaths == null) {
            extraPaths = Collections.unmodifiableCollection(getRawExtraPathsPrivate(new HashSet<String>()));
            if (DEBUG) {
                System.out.println(getLocaleID() + "\textras: " + extraPaths.size());
            }
        }
        return extraPaths;
    }

    private Collection<String> getRawExtraPathsPrivate(Collection<String> toAddTo) {
        SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();
        // units
        PluralInfo plurals = supplementalData.getPlurals(PluralType.cardinal, getLocaleID());
        if (plurals == null && DEBUG) {
            System.err.println("No " + PluralType.cardinal + "  plurals for " + getLocaleID() + " in " + supplementalData.getDirectory().getAbsolutePath());
        }
        Set<Count> pluralCounts = null;
        if (plurals != null) {
            pluralCounts = plurals.getCounts();
            if (pluralCounts.size() != 1) {
                // we get all the root paths with count
                addPluralCounts(toAddTo, pluralCounts, this);
            }
        }
        // dayPeriods
        String locale = getLocaleID();
        DayPeriodInfo dayPeriods = supplementalData.getDayPeriods(DayPeriodInfo.Type.format, locale);
        if (dayPeriods != null) {
            LinkedHashSet<DayPeriod> items = new LinkedHashSet<DayPeriod>(dayPeriods.getPeriods());
            items.add(DayPeriod.am);
            items.add(DayPeriod.pm);
            for (String context : new String[] { "format", "stand-alone" }) {
                for (String width : new String[] { "narrow", "abbreviated", "wide" }) {
                    for (DayPeriod dayPeriod : items) {
                        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="am"]
                        toAddTo.add("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/" +
                            "dayPeriodContext[@type=\"" + context
                            + "\"]/dayPeriodWidth[@type=\"" + width
                            + "\"]/dayPeriod[@type=\"" + dayPeriod + "\"]");
                    }
                }
            }
        }

        // metazones
        Set<String> zones = supplementalData.getAllMetazones();

        for (String zone : zones) {
            for (String width : new String[] { "long", "short" }) {
                for (String type : new String[] { "generic", "standard", "daylight" }) {
                    toAddTo.add("//ldml/dates/timeZoneNames/metazone[@type=\"" + zone + "\"]/" + width + "/" + type);
                }
            }
        }

        // Individual zone overrides
        final String[] overrides = {
            "Pacific/Honolulu\"]/short/generic",
            "Pacific/Honolulu\"]/short/standard",
            "Pacific/Honolulu\"]/short/daylight",
            "Europe/Dublin\"]/long/daylight",
            "Europe/London\"]/long/daylight",
            "Etc/UTC\"]/long/standard",
            "Etc/UTC\"]/short/standard"
        };
        for (String override : overrides) {
            toAddTo.add("//ldml/dates/timeZoneNames/zone[@type=\"" + override);
        }

        // Currencies
        Set<String> codes = supplementalData.getBcp47Keys().getAll("cu");
        for (String code : codes) {
            String currencyCode = code.toUpperCase();
            toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + currencyCode + "\"]/symbol");
            toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + currencyCode + "\"]/displayName");
            if (pluralCounts != null) {
                for (Count count : pluralCounts) {
                    toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + currencyCode + "\"]/displayName[@count=\"" + count.toString() + "\"]");
                }
            }
        }

        return toAddTo;
    }

    private void showStars(String title, Iterable<String> source) {
        PathStarrer ps = new PathStarrer();
        Relation<String, String> stars = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String path : source) {
            String skeleton = ps.set(path);
            stars.put(skeleton, ps.getAttributesString("|"));

        }
        System.out.println(title);
        for (Entry<String, Set<String>> s : stars.keyValuesSet()) {
            System.out.println("\t" + s.getKey() + "\t" + s.getValue());
        }
    }

    private void addPluralCounts(Collection<String> toAddTo,
        final Set<Count> pluralCounts,
        Iterable<String> file) {
        for (String path : file) {
            String countAttr = "[@count=\"other\"]";
            int countPos = path.indexOf(countAttr);
            if (countPos < 0) {
                continue;
            }
            String start = path.substring(0, countPos) + "[@count=\"";
            String end = "\"]" + path.substring(countPos + countAttr.length());
            for (Count count : pluralCounts) {
                if (count == Count.other) {
                    continue;
                }
                toAddTo.add(start + count + end);

                //                for (String unit : new String[] { "year", "month", "week", "day", "hour", "minute", "second" }) {
                //                    for (String when : new String[] { "", "-past", "-future" }) {
                //                        toAddTo.add("//ldml/units/unit[@type=\"" + unit + when + "\"]/unitPattern[@count=\""
                //                            + count + "\"]");
                //                    }
                //                    for (String alt : new String[] { "", "[@alt=\"short\"]" }) {
                //                        toAddTo.add("//ldml/units/unit[@type=\"" + unit + "\"]/unitPattern[@count=\"" + count
                //                            + "\"]" + alt);
                //                    }
                //                }

                //                    for (String unit : codes) {
                //                        toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + unit + "\"]/displayName[@count=\""
                //                                + count + "\"]");
                //                    }
                //
                //                    for (String numberSystem : supplementalData.getNumericNumberingSystems()) {
                //                        String numberSystemString = "[@numberSystem=\"" + numberSystem + "\"]";
                //                        final String currencyPattern = "//ldml/numbers/currencyFormats" + numberSystemString +
                //                                "/unitPattern[@count=\"" + count + "\"]";
                //                        toAddTo.add(currencyPattern);
                //                        if (DEBUG) {
                //                            System.out.println(getLocaleID() + "\t" + currencyPattern);
                //                        }
                //
                //                        for (String type : new String[] {
                //                                "1000", "10000", "100000", "1000000", "10000000", "100000000", "1000000000",
                //                                "10000000000", "100000000000", "1000000000000", "10000000000000", "100000000000000" }) {
                //                            for (String width : new String[] { "short", "long" }) {
                //                                toAddTo.add("//ldml/numbers/decimalFormats" +
                //                                        numberSystemString + "/decimalFormatLength[@type=\"" +
                //                                        width + "\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"" +
                //                                        type + "\"][@count=\"" +
                //                                        count + "\"]");
                //                            }
                //                        }
                //                    }
            }
        }
    }

    // This code never worked right, since extraPaths is static.
    // private boolean addUnlessValueEmpty(final String path, Collection<String> toAddTo) {
    // String value = getWinningValue(path);
    // if (value != null && value.length() == 0) {
    // return false;
    // } else {
    // toAddTo.add(path);
    // return true;
    // }
    // }

    private Matcher typeValueMatcher = PatternCache.get("\\[@type=\"([^\"]*)\"\\]").matcher("");

    public boolean isPathExcludedForSurvey(String distinguishedPath) {
        // for now, just zones
        if (distinguishedPath.contains("/exemplarCity")) {
            excludedZones = getExcludedZones();
            typeValueMatcher.reset(distinguishedPath).find();
            if (excludedZones.contains(typeValueMatcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> excludedZones;

    public Set<String> getExcludedZones() {
        synchronized (this) {
            if (excludedZones == null) {
                SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();
                // SupplementalDataInfo.getInstance(getSupplementalDirectory());
                excludedZones = new HashSet<String>(supplementalData.getSingleRegionZones());
                excludedZones = Collections.unmodifiableSet(excludedZones); // protect
            }
            return excludedZones;
        }
    }

    /**
     * Get the path with the given count.
     * It acts like there is an alias in root from count=n to count=one,
     * then for currency display names from count=one to no count <br>
     * For unitPatterns, falls back to Count.one. <br>
     * For others, falls back to Count.one, then no count.
     * <p>
     * The fallback acts like an alias in root.
     *
     * @param xpath
     * @param count
     *            Count may be null. Returns null if nothing is found.
     * @param winning
     *            TODO
     * @return
     */
    public String getCountPathWithFallback(String xpath, Count count, boolean winning) {
        String result;
        XPathParts parts = XPathParts.getTestInstance(xpath);
        boolean isDisplayName = parts.contains("displayName");

        String intCount = parts.getAttributeValue(-1, "count");
        if (intCount != null && CldrUtility.DIGITS.containsAll(intCount)) {
            try {
                int item = Integer.parseInt(intCount);
                String locale = getLocaleID();
                // TODO get data from SupplementalDataInfo...
                PluralRules rules = PluralRules.forLocale(new ULocale(locale));
                String keyword = rules.select(item);
                Count itemCount = Count.valueOf(keyword);
                result = getCountPathWithFallback2(parts, xpath, itemCount, winning);
                if (result != null && isNotRoot(result)) {
                    return result;
                }
            } catch (NumberFormatException e) {
            }
        }

        // try the given count first
        result = getCountPathWithFallback2(parts, xpath, count, winning);
        if (result != null && isNotRoot(result)) {
            return result;
        }
        // now try fallback
        if (count != Count.other) {
            result = getCountPathWithFallback2(parts, xpath, Count.other, winning);
            if (result != null && isNotRoot(result)) {
                return result;
            }
        }
        // now try deletion (for currency)
        if (isDisplayName) {
            result = getCountPathWithFallback2(parts, xpath, null, winning);
        }
        return result;
    }

    private String getCountPathWithFallback2(XPathParts parts, String xpathWithNoCount,
        Count count, boolean winning) {
        parts.addAttribute("count", count == null ? null : count.toString());
        String newPath = parts.toString();
        if (!newPath.equals(xpathWithNoCount)) {
            if (winning) {
                String temp = getWinningPath(newPath);
                if (temp != null) {
                    newPath = temp;
                }
            }
            if (dataSource.getValueAtPath(newPath) != null) {
                return newPath;
            }
            // return getWinningPath(newPath);
        }
        return null;
    }

    /**
     * Returns a value to be used for "filling in" a "Change" value in the survey
     * tool. Currently returns the following.
     * <ul>
     * <li>The "winning" value (if not inherited). Example: if "Donnerstag" has the most votes for 'thursday', then
     * clicking on the empty field will fill in "Donnerstag"
     * <li>The singular form. Example: if the value for 'hour' is "heure", then clicking on the entry field for 'hours'
     * will insert "heure".
     * <li>The parent's value. Example: if I'm in [de_CH] and there are no proposals for 'thursday', then clicking on
     * the empty field will fill in "Donnerstag" from [de].
     * <li>Otherwise don't fill in anything, and return null.
     * </ul>
     *
     * @return
     */
    public String getFillInValue(String distinguishedPath) {
        String winningPath = getWinningPath(distinguishedPath);
        if (isNotRoot(winningPath)) {
            return getStringValue(winningPath);
        }
        String fallbackPath = getFallbackPath(winningPath, true);
        if (fallbackPath != null) {
            String value = getWinningValue(fallbackPath);
            if (value != null) {
                return value;
            }
        }
        return getStringValue(winningPath);
    }

    /**
     * returns true if the source of the path exists, and is neither root nor code-fallback
     *
     * @param distinguishedPath
     * @return
     */
    public boolean isNotRoot(String distinguishedPath) {
        String source = getSourceLocaleID(distinguishedPath, null);
        return source != null && !source.equals("root") && !source.equals(XMLSource.CODE_FALLBACK_ID);
    }

    public boolean isAliasedAtTopLevel() {
        return iterator("//ldml/alias").hasNext();
    }

    public static Comparator<String> getComparator(DtdType dtdType) {
        if (dtdType == null) {
            return ldmlComparator;
        }
        switch (dtdType) {
        case ldml:
        case ldmlICU:
            return ldmlComparator;
        default:
            return DtdData.getInstance(dtdType).getDtdComparator(null);
        }
    }

    public Comparator<String> getComparator() {
        return getComparator(dtdType);
    }

    public DtdType getDtdType() {
        return dtdType != null ? dtdType
            : dataSource.getDtdType();
    }

    public DtdData getDtdData() {
        return dtdData != null ? dtdData
            : DtdData.getInstance(getDtdType());
    }

    public static Comparator<String> getPathComparator(String path) {
        DtdType fileDtdType = DtdType.fromPath(path);
        return getComparator(fileDtdType);
    }

    public static MapComparator<String> getAttributeOrdering() {
        return DtdData.getInstance(DtdType.ldmlICU).getAttributeComparator();
    }

    public CLDRFile getUnresolved() {
        if (!isResolved()) {
            return this;
        }
        XMLSource source = dataSource.getUnresolving();
        return new CLDRFile(source);
    }

    public static Comparator<String> getAttributeValueComparator(String element, String attribute) {
        return DtdData.getAttributeValueComparator(DtdType.ldml, element, attribute);
    }

    public void setDtdType(DtdType dtdType) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        this.dtdType = dtdType;
    }
}
