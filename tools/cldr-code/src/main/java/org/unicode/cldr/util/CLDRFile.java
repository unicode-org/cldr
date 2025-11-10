/*
 **********************************************************************
 * Copyright (c) 2002-2019, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.LocaleInheritanceInfo.Reason;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.With.SimpleIterator;
import org.unicode.cldr.util.XMLFileReader.AllHandler;
import org.unicode.cldr.util.XMLSource.ResolvingSource;
import org.unicode.cldr.util.XPathParts.Comments;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This is a class that represents the contents of a CLDR file, as <key,value> pairs, where the key
 * is a "cleaned" xpath (with non-distinguishing attributes removed), and the value is an object
 * that contains the full xpath plus a value, which is a string, or a node (the latter for atomic
 * elements).
 *
 * <p><b>WARNING: The API on this class is likely to change.</b> Having the full xpath on the value
 * is clumsy; I need to change it to having the key be an object that contains the full xpath, but
 * then sorts as if it were clean.
 *
 * <p>Each instance also contains a set of associated comments for each xpath.
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

public class CLDRFile implements Freezable<CLDRFile>, Iterable<String>, LocaleStringProvider {

    /**
     * Variable to control whether File reads are buffered; this will about halve the time spent in
     * loadFromFile() and Factory.make() from about 20 % to about 10 %. It will also noticeably
     * improve the different unit tests take in the TestAll fixture. TRUE - use buffering (default)
     * FALSE - do not use buffering
     */
    private static final boolean USE_LOADING_BUFFER = true;

    private static final boolean DEBUG = false;

    public static final Pattern ALT_PROPOSED_PATTERN =
            PatternCache.get(".*\\[@alt=\"[^\"]*proposed[^\"]*\"].*");
    public static final Pattern DRAFT_PATTERN = PatternCache.get("\\[@draft=\"([^\"]*)\"\\]");
    public static final Pattern XML_SPACE_PATTERN =
            PatternCache.get("\\[@xml:space=\"([^\"]*)\"\\]");

    private static final boolean LOG_PROGRESS = false;

    public static boolean HACK_ORDER = false;
    private static final boolean DEBUG_LOGGING = false;

    public static final String SUPPLEMENTAL_NAME = "supplementalData";

    public static final String GEN_VERSION = "49";
    public static final List<String> SUPPLEMENTAL_NAMES =
            Arrays.asList(
                    "characters",
                    "coverageLevels",
                    "dayPeriods",
                    "genderList",
                    "grammaticalFeatures",
                    "languageInfo",
                    "languageGroup",
                    "likelySubtags",
                    "metaZones",
                    "numberingSystems",
                    "ordinals",
                    "pluralRanges",
                    "plurals",
                    "postalCodeData",
                    "rgScope",
                    "supplementalData",
                    "supplementalMetadata",
                    "telephoneCodeData",
                    "units",
                    "windowsZones");

    private Set<String> extraPaths = null;

    private boolean locked;
    private DtdType dtdType;
    private DtdData dtdData;

    XMLSource dataSource; // TODO(jchye): make private

    private File supplementalDirectory;

    /**
     * Does the value in question either match or inherent the current value?
     *
     * <p>To match, the value in question and the current value must be non-null and equal.
     *
     * <p>To inherit the current value, the value in question must be INHERITANCE_MARKER and the
     * current value must equal the bailey value.
     *
     * <p>This CLDRFile is only used here for getBaileyValue, not to get curValue
     *
     * @param value the value in question
     * @param curValue the current value, that is, XMLSource.getValueAtDPath(xpathString)
     * @param xpathString the path identifier
     * @return true if it matches or inherits, else false
     */
    public boolean equalsOrInheritsCurrentValue(String value, String curValue, String xpathString) {
        if (value == null || curValue == null) {
            return false;
        }
        if (value.equals(curValue)) {
            return true;
        }
        if (value.equals(CldrUtility.INHERITANCE_MARKER)) {
            String baileyValue = getBaileyValue(xpathString, null, null);
            if (baileyValue == null) {
                /* This may happen for Invalid XPath; InvalidXPathException may be thrown. */
                return false;
            }
            if (curValue.equals(baileyValue)) {
                return true;
            }
        }
        return false;
    }

    public XMLSource getResolvingDataSource() {
        if (!isResolved()) {
            throw new IllegalArgumentException(
                    "CLDRFile must be resolved for getResolvingDataSource");
        }
        // dataSource instanceof XMLSource.ResolvingSource
        return dataSource;
    }

    public enum DraftStatus {
        unconfirmed,
        provisional,
        contributed,
        approved;

        public static DraftStatus forString(String string) {
            return string == null
                    ? DraftStatus.approved
                    : DraftStatus.valueOf(string.toLowerCase(Locale.ENGLISH));
        }

        /**
         * Get the draft status from a full xpath
         *
         * @param xpath
         * @return
         */
        public static DraftStatus forXpath(String xpath) {
            final String status =
                    XPathParts.getFrozenInstance(xpath).getAttributeValue(-1, "draft");
            return forString(status);
        }

        /** Return the XPath suffix for this draft status or "" for approved. */
        public String asXpath() {
            if (this == approved) {
                return "";
            } else {
                return "[@draft=\"" + name() + "\"]";
            }
        }

        /** update this XPath with this draft status */
        public String updateXPath(final String fullXpath) {
            final XPathParts xpp = XPathParts.getFrozenInstance(fullXpath).cloneAsThawed();
            final String oldDraft = xpp.getAttributeValue(-1, "draft");
            if (forString(oldDraft) == this) {
                return fullXpath; // no change;
            }
            if (this == approved) {
                xpp.removeAttribute(-1, "draft");
            } else {
                xpp.setAttribute(-1, "draft", this.name());
            }
            return xpp.toString();
        }
    }

    @Override
    public String toString() {
        return "{"
                + "locked="
                + locked
                + " locale="
                + dataSource.getLocaleID()
                + " dataSource="
                + dataSource.toString()
                + "}";
    }

    public String toString(String regex) {
        return "{"
                + "locked="
                + locked
                + " locale="
                + dataSource.getLocaleID()
                + " regex="
                + regex
                + " dataSource="
                + dataSource.toString(regex)
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

    private final NameGetter nameGetter;

    private static final boolean DEBUG_CLDR_FILE = false;
    private String creationTime = null; // only used if DEBUG_CLDR_FILE

    /**
     * Construct a new CLDRFile.
     *
     * @param dataSource must not be null
     */
    public CLDRFile(XMLSource dataSource) {
        this.dataSource = dataSource;
        this.nameGetter = new NameGetter(this);

        if (DEBUG_CLDR_FILE) {
            creationTime =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(Calendar.getInstance().getTime());
            System.out.println("📂 Created new CLDRFile(dataSource) at " + creationTime);
        }
    }

    /**
     * get Unresolved CLDRFile
     *
     * @param localeId
     * @param dirs
     * @param minimalDraftStatus
     */
    public CLDRFile(String localeId, List<File> dirs, DraftStatus minimalDraftStatus) {
        this.nameGetter = new NameGetter(this);
        // order matters
        this.dataSource = XMLSource.getFrozenInstance(localeId, dirs, minimalDraftStatus);
        this.dtdType = dataSource.getXMLNormalizingDtdType();
        this.dtdData = DtdData.getInstance(this.dtdType);
    }

    public CLDRFile(XMLSource dataSource, XMLSource... resolvingParents) {
        this.nameGetter = new NameGetter(this);
        List<XMLSource> sourceList = new ArrayList<>();
        sourceList.add(dataSource);
        sourceList.addAll(Arrays.asList(resolvingParents));
        this.dataSource = new ResolvingSource(sourceList);

        if (DEBUG_CLDR_FILE) {
            creationTime =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(Calendar.getInstance().getTime());
            System.out.println(
                    "📂 Created new CLDRFile(dataSource, XMLSource... resolvingParents) at "
                            + creationTime);
        }
    }

    public NameGetter nameGetter() {
        return nameGetter;
    }

    public static CLDRFile loadFromFile(
            File f, String localeName, DraftStatus minimalDraftStatus, XMLSource source) {
        String fullFileName = f.getAbsolutePath();
        try {
            fullFileName = PathUtilities.getNormalizedPathString(f);
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
                try (InputStream fis = new FileInputStream(f)) {
                    cldrFile = load(fullFileName, localeName, fis, minimalDraftStatus, source);
                    return cldrFile;
                }
            }

        } catch (Exception e) {
            // use a StringBuilder to construct the message.
            String sb = "Cannot read the file '" + fullFileName + "': " + e.getMessage();
            throw new ICUUncheckedIOException(sb, e);
        }
    }

    public static CLDRFile loadFromFiles(
            List<File> dirs, String localeName, DraftStatus minimalDraftStatus, XMLSource source) {
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
                        cldrFile.loadFromInputStream(
                                PathUtilities.getNormalizedPathString(f),
                                localeName,
                                fis,
                                minimalDraftStatus,
                                false);
                    }
                }
                return cldrFile;
            } else {
                throw new IllegalArgumentException("Must use USE_LOADING_BUFFER");
            }

        } catch (Exception e) {
            // e.printStackTrace();
            throw new ICUUncheckedIOException("Cannot read the file '" + dirs, e);
        }
    }

    /**
     * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to
     * create CLDRFiles.)
     *
     * @param f
     * @param localeName
     * @param minimalDraftStatus
     */
    public static CLDRFile loadFromFile(File f, String localeName, DraftStatus minimalDraftStatus) {
        return loadFromFile(f, localeName, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    public static CLDRFile loadFromFiles(
            List<File> dirs, String localeName, DraftStatus minimalDraftStatus) {
        return loadFromFiles(dirs, localeName, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    static CLDRFile load(
            String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
        return load(fileName, localeName, fis, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    /**
     * Load a CLDRFile from a file input stream.
     *
     * @param localeName
     * @param fis
     */
    private static CLDRFile load(
            String fileName,
            String localeName,
            InputStream fis,
            DraftStatus minimalDraftStatus,
            XMLSource source) {
        CLDRFile cldrFile = new CLDRFile(source);
        return cldrFile.loadFromInputStream(fileName, localeName, fis, minimalDraftStatus, false);
    }

    /**
     * Load a CLDRFile from a file input stream.
     *
     * @param localeName
     * @param fis
     */
    private static CLDRFile load(
            String fileName,
            String localeName,
            InputStream fis,
            DraftStatus minimalDraftStatus,
            XMLSource source,
            boolean leniency) {
        CLDRFile cldrFile = new CLDRFile(source);
        return cldrFile.loadFromInputStream(
                fileName, localeName, fis, minimalDraftStatus, leniency);
    }

    static CLDRFile load(
            String fileName,
            String localeName,
            InputStream fis,
            DraftStatus minimalDraftStatus,
            boolean leniency) {
        return load(
                fileName,
                localeName,
                fis,
                minimalDraftStatus,
                new SimpleXMLSource(localeName),
                leniency);
    }

    /**
     * Low-level function, only normally used for testing.
     *
     * @param fileName
     * @param localeName
     * @param fis
     * @param minimalDraftStatus
     * @param leniency if true, skip dtd validation
     * @return
     */
    public CLDRFile loadFromInputStream(
            String fileName,
            String localeName,
            InputStream fis,
            DraftStatus minimalDraftStatus,
            boolean leniency) {
        CLDRFile cldrFile = this;
        MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(cldrFile, minimalDraftStatus);
        XMLFileReader.read(fileName, fis, -1, !leniency, DEFAULT_DECLHANDLER);
        if (DEFAULT_DECLHANDLER.isSupplemental < 0) {
            throw new IllegalArgumentException(
                    "root of file must be either ldml or supplementalData");
        }
        cldrFile.setNonInheriting(DEFAULT_DECLHANDLER.isSupplemental > 0);
        if (DEFAULT_DECLHANDLER.overrideCount > 0) {
            throw new IllegalArgumentException(
                    "Internal problems: either data file has duplicate path, or"
                            + " CLDRFile.isDistinguishing() or CLDRFile.isOrdered() need updating: "
                            + DEFAULT_DECLHANDLER.overrideCount
                            + "; The exact problems are printed on the console above.");
        }
        if (localeName == null) {
            cldrFile.dataSource.setLocaleID(cldrFile.getLocaleIDFromIdentity());
        }
        return cldrFile;
    }

    /**
     * Clone the object. Produces unlocked version
     *
     * @see com.ibm.icu.util.Freezable
     */
    @Override
    public CLDRFile cloneAsThawed() {
        try {
            CLDRFile result = (CLDRFile) super.clone();
            result.locked = false;
            result.dataSource = result.dataSource.cloneAsThawed();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("should never happen");
        }
    }

    /** Prints the contents of the file (the xpaths/values) to the console. */
    public CLDRFile show() {
        for (String xpath : this) {
            System.out.println(getFullXPath(xpath) + " =>\t" + getStringValue(xpath));
        }
        return this;
    }

    private static final Map<String, Object> nullOptions =
            Collections.unmodifiableMap(new TreeMap<>());

    /**
     * Write the corresponding XML file out, with the normal formatting and indentation. Will update
     * the identity element, including version, and other items. If the CLDRFile is empty, the DTD
     * type will be //ldml.
     */
    public void write(PrintWriter pw) {
        write(pw, nullOptions);
    }

    /**
     * Write the corresponding XML file out, with the normal formatting and indentation. Will update
     * the identity element, including version, and other items. If the CLDRFile is empty, the DTD
     * type will be //ldml.
     *
     * @param pw writer to print to
     * @param options map of options for writing
     * @return true if we write the file, false if we cancel due to skipping all paths
     */
    public boolean write(PrintWriter pw, Map<String, ?> options) {
        final CldrXmlWriter xmlWriter = new CldrXmlWriter(this, pw, options);
        xmlWriter.write();
        return true;
    }

    /** Get a string value from an xpath. */
    @Override
    public String getStringValue(String xpath) {
        try {
            String result = dataSource.getValueAtPath(xpath);
            if (result == null && dataSource.isResolving()) {
                final String fallbackPath = getFallbackPath(xpath, false, true);
                // often fallbackPath equals xpath -- in such cases, isn't it a waste of time to
                // call getValueAtPath again?
                if (fallbackPath != null) {
                    result = dataSource.getValueAtPath(fallbackPath);
                }
            }
            // Note: the following can occur even when result != null at this point, and it can
            // improve the result.For example, the code above may give "zh_Hans (FONIPA)", while the
            // constructed value gotten below is "xitoy [soddalashgan] (FONIPA)" (in locale uz),
            // which is expected by TestLocaleDisplay.
            if (isResolved()
                    && GlossonymConstructor.valueIsBogus(result)
                    && GlossonymConstructor.pathIsEligible(xpath)) {
                final String constructedValue = new GlossonymConstructor(this).getValue(xpath);
                if (constructedValue != null) {
                    result = constructedValue;
                }
            }
            return result;
        } catch (Exception e) {
            throw new UncheckedExecutionException("Bad path: " + xpath, e);
        }
    }

    /**
     * Get GeorgeBailey value: that is, what the value would be if it were not directly contained in
     * the file at that path. If the value is null or INHERITANCE_MARKER (with resolving), then
     * baileyValue = resolved value. A non-resolving CLDRFile will always return null.
     */
    public String getBaileyValue(
            String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        String result = dataSource.getBaileyValue(xpath, pathWhereFound, localeWhereFound);
        if ((result == null || result.equals(CldrUtility.INHERITANCE_MARKER))
                && dataSource.isResolving()) {
            final String fallbackPath =
                    getFallbackPath(
                            xpath, false,
                            false); // return null if there is no different sideways path
            if (xpath.equals(fallbackPath)) {
                getFallbackPath(xpath, false, true);
                throw new IllegalArgumentException(); // should never happen
            }
            if (fallbackPath != null) {
                result = dataSource.getValueAtPath(fallbackPath);
                if (result != null) {
                    Status status = new Status();
                    if (localeWhereFound != null) {
                        localeWhereFound.value = dataSource.getSourceLocaleID(fallbackPath, status);
                    }
                    if (pathWhereFound != null) {
                        pathWhereFound.value = status.pathWhereFound;
                    }
                }
            }
        }
        if (isResolved()
                && GlossonymConstructor.valueIsBogus(result)
                && GlossonymConstructor.pathIsEligible(xpath)) {
            final GlossonymConstructor gc = new GlossonymConstructor(this);
            final String constructedValue =
                    gc.getValueAndTrack(xpath, pathWhereFound, localeWhereFound);
            if (constructedValue != null) {
                result = constructedValue;
            }
        }
        return result;
    }

    /**
     * Return a list of all paths which contributed to the value, as well as all bailey values. This
     * is used to explain inheritance and bailey values. The list must be interpreted in order. When
     * {@link LocaleInheritanceInfo.Reason#isTerminal()} return true, that indicates a successful
     * lookup and partitions values from subsequent bailey values.
     *
     * @see #getBaileyValue(String, Output, Output)
     * @see #getSourceLocaleIdExtended(String, Status, boolean)
     */
    public List<LocaleInheritanceInfo> getPathsWhereFound(String xpath) {
        if (!isResolved()) {
            throw new IllegalArgumentException(
                    "getPathsWhereFound() is only valid on a resolved CLDRFile");
        }
        LinkedList<LocaleInheritanceInfo> list = new LinkedList<>();
        // first, call getSourceLocaleIdExtended to populate the list
        Status status = new Status();
        getSourceLocaleIdExtended(xpath, status, false, list);
        final String path1 = status.pathWhereFound;
        // For now, the only special case is Glossonym
        if (path1.equals(GlossonymConstructor.PSEUDO_PATH)) {
            // it's a Glossonym, so as the GlossonymConstructor what the paths are.  Sort paths in
            // reverse order.
            final Set<String> xpaths =
                    new GlossonymConstructor(this)
                            .getPathsWhereFound(xpath, new TreeSet<>(Comparator.reverseOrder()));
            for (final String subpath : xpaths) {
                final String locale2 = getSourceLocaleIdExtended(subpath, status, true);
                final String path2 = status.pathWhereFound;
                // Paths are in reverse order (c-b-a) so we insert them at the top of our list.
                list.addFirst(new LocaleInheritanceInfo(locale2, path2, Reason.constructed));
            }

            // now the list contains:
            // constructed: a
            // constructed: b
            // constructed: c
            // (none) - this is where the glossonym was
            // (bailey value(s))
        }
        return list;
    }

    static final class SimpleAltPicker implements Transform<String, String> {
        public final String alt;

        public SimpleAltPicker(String alt) {
            this.alt = alt;
        }

        @Override
        public String transform(@SuppressWarnings("unused") String source) {
            return alt;
        }
    }

    /**
     * Only call if xpath doesn't exist in the current file.
     *
     * <p>For now, just handle counts and cases: see getCountPath Also handle extraPaths
     *
     * @param xpath
     * @param winning TODO
     * @param checkExtraPaths TODO
     * @return
     */
    private String getFallbackPath(String xpath, boolean winning, boolean checkExtraPaths) {
        if (GrammaticalFeature.pathHasFeature(xpath) != null) {
            return getCountPathWithFallback(xpath, Count.other, winning);
        }
        if (checkExtraPaths && getRawExtraPaths().contains(xpath)) {
            return xpath;
        }
        return null;
    }

    /**
     * Get the full path from a distinguished path.
     *
     * @param xpath the distinguished path
     * @return the full path
     *     <p>Examples:
     *     <p>xpath = //ldml/localeDisplayNames/scripts/script[@type="Adlm"] result =
     *     //ldml/localeDisplayNames/scripts/script[@type="Adlm"][@draft="unconfirmed"]
     *     <p>xpath =
     *     //ldml/dates/calendars/calendar[@type="hebrew"]/dateFormats/dateFormatLength[@type="full"]/dateFormat[@type="standard"]/pattern[@type="standard"]
     *     result =
     *     //ldml/dates/calendars/calendar[@type="hebrew"]/dateFormats/dateFormatLength[@type="full"]/dateFormat[@type="standard"]/pattern[@type="standard"][@numbers="hebr"]
     */
    public String getFullXPath(String xpath) {
        if (xpath == null) {
            throw new NullPointerException("Null distinguishing xpath");
        }
        String result = dataSource.getFullPath(xpath);
        return result != null
                ? result
                : xpath; // we can't add any non-distinguishing values if there is nothing there.
    }

    /**
     * Get the last modified date (if available) from a distinguished path.
     *
     * @return date or null if not available.
     */
    public Date getLastModifiedDate(String xpath) {
        return dataSource.getChangeDateAtDPath(xpath);
    }

    /**
     * Find out where the value was found (for resolving locales). Returns {@link
     * XMLSource#CODE_FALLBACK_ID} as the location if nothing is found
     *
     * @param distinguishedXPath path (must be distinguished!)
     * @param status the distinguished path where the item was found. Pass in null if you don't
     *     care.
     */
    @Override
    public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
        return getSourceLocaleIdExtended(
                distinguishedXPath, status, true /* skipInheritanceMarker */);
    }

    /**
     * Find out where the value was found (for resolving locales). Returns {@link
     * XMLSource#CODE_FALLBACK_ID} as the location if nothing is found
     *
     * @param distinguishedXPath path (must be distinguished!)
     * @param status the distinguished path where the item was found. Pass in null if you don't
     *     care.
     * @param skipInheritanceMarker if true, skip sources in which value is INHERITANCE_MARKER
     * @return the locale id as a string
     */
    public String getSourceLocaleIdExtended(
            String distinguishedXPath, CLDRFile.Status status, boolean skipInheritanceMarker) {
        return getSourceLocaleIdExtended(distinguishedXPath, status, skipInheritanceMarker, null);
    }

    public String getSourceLocaleIdExtended(
            String distinguishedXPath,
            CLDRFile.Status status,
            boolean skipInheritanceMarker,
            List<LocaleInheritanceInfo> list) {
        String result =
                dataSource.getSourceLocaleIdExtended(
                        distinguishedXPath, status, skipInheritanceMarker, list);
        if (XMLSource.CODE_FALLBACK_ID.equals(result) && dataSource.isResolving()) {
            final String fallbackPath = getFallbackPath(distinguishedXPath, false, true);
            if (fallbackPath != null && !fallbackPath.equals(distinguishedXPath)) {
                if (list != null) {
                    list.add(
                            new LocaleInheritanceInfo(
                                    getLocaleID(), distinguishedXPath, Reason.fallback, null));
                }
                result =
                        dataSource.getSourceLocaleIdExtended(
                                fallbackPath, status, skipInheritanceMarker, list);
            }
            if (XMLSource.CODE_FALLBACK_ID.equals(result)
                    && getConstructedValue(distinguishedXPath) != null) {
                if (status != null) {
                    status.pathWhereFound = GlossonymConstructor.PSEUDO_PATH;
                }
                return getLocaleID();
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
        Log.logln(
                LOG_PROGRESS,
                "ADDING: \t" + currentFullXPath + " \t" + value + "\t" + currentFullXPath);
        // xpath = xpath.intern();
        try {
            dataSource.putValueAtPath(currentFullXPath, value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "failed adding " + currentFullXPath + ",\t" + value, e);
        }
        return this;
    }

    /** Note where this element was parsed. */
    public CLDRFile addSourceLocation(String currentFullXPath, XMLSource.SourceLocation location) {
        dataSource.addSourceLocation(currentFullXPath, location);
        return this;
    }

    /**
     * Get the line and column for a path
     *
     * @param path xpath or fullpath
     */
    public XMLSource.SourceLocation getSourceLocation(String path) {
        final String fullPath = getFullXPath(path);
        return dataSource.getSourceLocation(fullPath);
    }

    public CLDRFile addComment(String xpath, String comment, Comments.CommentType type) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        // System.out.println("Adding comment: <" + xpath + "> '" + comment + "'");
        Log.logln(LOG_PROGRESS, "ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
        if (xpath == null || xpath.isEmpty()) {
            dataSource
                    .getXpathComments()
                    .setFinalComment(
                            CldrUtility.joinWithSeparation(
                                    dataSource.getXpathComments().getFinalComment(),
                                    XPathParts.NEWLINE,
                                    comment));
        } else {
            xpath = getDistinguishingXPath(xpath, null);
            dataSource.getXpathComments().addComment(type, xpath, comment);
        }
        return this;
    }

    // TODO Change into enum, update docs
    public static final int MERGE_KEEP_MINE = 0,
            MERGE_REPLACE_MINE = 1,
            MERGE_ADD_ALTERNATE = 2,
            MERGE_REPLACE_MY_DRAFT = 3;

    /**
     * Merges elements from another CLDR file. Note: when both have the same xpath key, the keepMine
     * determines whether "my" values are kept or the other files values are kept.
     *
     * @param other
     * @param conflict_resolution
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
            Set<String> hasDraftVersion = new HashSet<>();
            for (String cpath : dataSource) {
                String fullpath = getFullXPath(cpath);
                if (fullpath.contains("[@draft")) {
                    hasDraftVersion.add(
                            getNondraftNonaltXPath(cpath)); // strips the alt and the draft
                }
            }
            // only replace draft items!
            // this is either an item with draft in the fullpath
            // or an item with draft and alt in the full path
            for (String cpath : other) {
                cpath = getNondraftNonaltXPath(cpath);
                String newValue = other.getStringValue(cpath);
                String newFullPath = getNondraftNonaltXPath(other.getFullXPath(cpath));
                // another hack; need to add references back in
                newFullPath = addReferencesIfNeeded(newFullPath, getFullXPath(cpath));

                if (!hasDraftVersion.contains(cpath)) {
                    if (cpath.startsWith("//ldml/identity/"))
                        continue; // skip, since the error msg is not needed.
                    String myVersion = getStringValue(cpath);
                    if (myVersion == null || !newValue.equals(myVersion)) {
                        Log.logln(
                                getLocaleID()
                                        + "\tDenied attempt to replace non-draft"
                                        + CldrUtility.LINE_SEPARATOR
                                        + "\tcurr: ["
                                        + cpath
                                        + ",\t"
                                        + myVersion
                                        + "]"
                                        + CldrUtility.LINE_SEPARATOR
                                        + "\twith: ["
                                        + newValue
                                        + "]");
                        continue;
                    }
                }
                Log.logln(getLocaleID() + "\tVETTED: [" + newFullPath + ",\t" + newValue + "]");
                dataSource.putValueAtPath(newFullPath, newValue);
            }
        } else if (conflict_resolution == MERGE_ADD_ALTERNATE) {
            for (String key : other) {
                String otherValue = other.getStringValue(key);
                String myValue = dataSource.getValueAtPath(key);
                if (myValue == null) {
                    dataSource.putValueAtPath(other.getFullXPath(key), otherValue);
                } else if (!(myValue.equals(otherValue)
                                && equalsIgnoringDraft(getFullXPath(key), other.getFullXPath(key)))
                        && !key.startsWith("//ldml/identity")) {
                    for (int i = 0; ; ++i) {
                        String prop = "proposed" + (i == 0 ? "" : String.valueOf(i));
                        XPathParts parts =
                                XPathParts.getFrozenInstance(other.getFullXPath(key))
                                        .cloneAsThawed(); // not frozen, for addAttribut
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

        dataSource
                .getXpathComments()
                .setInitialComment(
                        CldrUtility.joinWithSeparation(
                                dataSource.getXpathComments().getInitialComment(),
                                XPathParts.NEWLINE,
                                other.dataSource.getXpathComments().getInitialComment()));
        dataSource
                .getXpathComments()
                .setFinalComment(
                        CldrUtility.joinWithSeparation(
                                dataSource.getXpathComments().getFinalComment(),
                                XPathParts.NEWLINE,
                                other.dataSource.getXpathComments().getFinalComment()));
        dataSource.getXpathComments().joinAll(other.dataSource.getXpathComments());
        return this;
    }

    /** */
    private String addReferencesIfNeeded(String newFullPath, String fullXPath) {
        if (fullXPath == null || !fullXPath.contains("[@references=")) {
            return newFullPath;
        }
        XPathParts parts = XPathParts.getFrozenInstance(fullXPath);
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
        XPathParts newParts = XPathParts.getFrozenInstance(newFullPath);
        Map<String, String> attributes = newParts.getAttributes(newParts.size() - 1);
        String references = attributes.get("references");
        if (references == null) references = accummulatedReferences;
        else references += ", " + accummulatedReferences;
        attributes.put("references", references);
        System.out.println("Changing " + newFullPath + " plus " + fullXPath + " to " + newParts);
        return newParts.toString();
    }

    /** Removes an element from a CLDRFile. */
    public CLDRFile remove(String xpath) {
        remove(xpath, false);
        return this;
    }

    /** Removes an element from a CLDRFile. */
    public CLDRFile remove(String xpath, boolean butComment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        if (butComment) {
            appendFinalComment(
                    dataSource.getFullPath(xpath) + "::<" + dataSource.getValueAtPath(xpath) + ">");
        }
        dataSource.removeValueAtPath(xpath);
        return this;
    }

    /** Removes all xpaths from a CLDRFile. */
    public CLDRFile removeAll(Set<String> xpaths, boolean butComment) {
        if (butComment) appendFinalComment("Illegal attributes removed:");
        for (String xpath : xpaths) {
            remove(xpath, butComment);
        }
        return this;
    }

    private static final boolean MINIMIZE_ALT_PROPOSED = false;

    public interface RetentionTest {
        enum Retention {
            RETAIN,
            REMOVE,
            RETAIN_IF_DIFFERENT
        }

        Retention getRetention(String path);
    }

    /** Removes all items with same value */
    public CLDRFile removeDuplicates(
            CLDRFile other,
            boolean butComment,
            RetentionTest keepIfMatches,
            Collection<String> removedItems) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        // Matcher specialPathMatcher = dontRemoveSpecials ? specialsToKeep.matcher("") : null;
        boolean first = true;
        if (removedItems == null) {
            removedItems = new ArrayList<>();
        } else {
            removedItems.clear();
        }
        Set<String> checked = new HashSet<>();
        for (String curXpath : this) { // see what items we have that the other also has
            boolean logicDuplicate = true;

            if (!checked.contains(curXpath)) {
                // we compare logic Group and only remove when all are duplicate
                Set<String> logicGroups = LogicalGrouping.getPaths(this, curXpath);
                if (logicGroups != null) {
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
                }
                // we can't remove right away, since that disturbs the iterator.
                if (logicGroups != null) {
                    checked.addAll(logicGroups);
                    if (logicDuplicate) {
                        removedItems.addAll(logicGroups);
                    }
                }
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
     * @return Returns the locale ID. In the case of a supplemental data file, it is
     *     SUPPLEMENTAL_NAME.
     */
    @Override
    public String getLocaleID() {
        return dataSource.getLocaleID();
    }

    /**
     * @return the Locale ID, as declared in the //ldml/identity element
     */
    public String getLocaleIDFromIdentity() {
        ULocale.Builder lb = new ULocale.Builder();
        for (Iterator<String> i = iterator("//ldml/identity/"); i.hasNext(); ) {
            XPathParts xpp = XPathParts.getFrozenInstance(i.next());
            String k = xpp.getElement(-1);
            String v = xpp.getAttributeValue(-1, "type");
            switch (k) {
                case "language":
                    lb.setLanguage(v);
                    break;
                case "script":
                    lb.setScript(v);
                    break;
                case "territory":
                    lb.setRegion(v);
                    break;
                case "variant":
                    lb.setVariant(v);
                    break;
            }
        }
        return lb.build().toString(); // TODO: CLDRLocale ?
    }

    /**
     * Create xpaths for DateFormat that look like
     *
     * <pre>
     * //ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/pattern[@type="standard"]
     * //ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/pattern[@type="standard"][@numbers="*"]
     * </pre>
     *
     * @param calendar Calendar system identifier
     * @param length full, long, medium, short. "*" is a wildcard selector for XPath
     * @return
     */
    private String getDateFormatXpath(String calendar, String length) {
        String formatPattern =
                "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateFormats/dateFormatLength[@type=\"%s\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        return String.format(formatPattern, calendar, length);
    }

    /**
     * Create xpaths for DateFormat that look like
     *
     * <pre>
     * //ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/datetimeSkeleton[@type="standard"]
     * //ldml/dates/calendars/calendar[@type="*"]/dateFormats/dateFormatLength[@type="*"]/dateFormat[@type="standard"]/datetimeSkeleton[@type="standard"][@numbers="*"]
     * </pre>
     *
     * @param calendar Calendar system identifier
     * @param length full, long, medium, short. "*" is a wildcard selector for XPath
     * @return
     */
    private String getDateSkeletonXpath(String calendar, String length) {
        String formatPattern =
                "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateFormats/dateFormatLength[@type=\"%s\"]/dateFormat[@type=\"standard\"]/datetimeSkeleton";
        return String.format(formatPattern, calendar, length);
    }

    /**
     * Create xpaths for TimeFormat that look like
     *
     * <pre>
     * //ldml/dates/calendars/calendar[@type="*"]/timeFormats/timeFormatLength[@type="*"]/timeFormat[@type="standard"]/pattern[@type="standard"]
     * //ldml/dates/calendars/calendar[@type="*"]/timeFormats/timeFormatLength[@type="*"]/timeFormat[@type="standard"]/pattern[@type="standard"][@numbers="*"] // not currently used
     * </pre>
     *
     * @param calendar Calendar system idenfitier
     * @param length full, long, medium, short. "*" is a wildcard selector for XPath
     * @return
     */
    private String getTimeFormatXpath(String calendar, String length) {
        String formatPattern =
                "//ldml/dates/calendars/calendar[@type=\"%s\"]/timeFormats/timeFormatLength[@type=\"%s\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        return String.format(formatPattern, calendar, length);
    }

    /**
     * Create xpaths for the glue pattern from DateTimeFormat that look like
     *
     * <pre>
     *   //ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"]
     *   //ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="atTime"]/pattern[@type="standard"]
     *   //ldml/dates/calendars/calendar[@type="*"]/dateTimeFormats/dateTimeFormatLength[@type="*"]/dateTimeFormat[@type="relative"]/pattern[@type="standard"]
     * </pre>
     *
     * @param calendar
     * @param length
     * @param formatType "standard" or "atTime" or "relative"
     * @return
     */
    private String getDateTimeFormatXpath(String calendar, String length, String formatType) {
        String formatPattern =
                "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateTimeFormats/dateTimeFormatLength[@type=\"%s\"]/dateTimeFormat[@type=\"%s\"]/pattern[@type=\"standard\"]";
        return String.format(formatPattern, calendar, length, formatType);
    }

    public SimpleDateFormat getDateFormat(
            String calendar, String length, ICUServiceBuilder icuServiceBuilder) {
        String dateFormatXPath = // Get standard dateFmt for same calendar & length as this
                // dateTimePattern
                this.getDateFormatXpath(calendar, length);
        String dateFormatValue = this.getWinningValue(dateFormatXPath);

        if (dateFormatValue == null) {
            return null;
        }

        XPathParts parts = XPathParts.getFrozenInstance(this.getFullXPath(dateFormatXPath));
        String dateNumbersOverride = parts.findAttributeValue("pattern", "numbers");
        return icuServiceBuilder.getDateFormat(calendar, dateFormatValue, dateNumbersOverride);
    }

    public SimpleDateFormat getTimeFormat(
            String calendar, String length, ICUServiceBuilder icuServiceBuilder) {
        String timeFormatXPath = this.getTimeFormatXpath(calendar, length);
        String timeFormatValue = this.getWinningValue(timeFormatXPath);

        if (timeFormatValue == null) {
            return null;
        }

        XPathParts parts = XPathParts.getFrozenInstance(this.getFullXPath(timeFormatXPath));
        String timeNumbersOverride = parts.findAttributeValue("pattern", "numbers");
        return icuServiceBuilder.getDateFormat(calendar, timeFormatValue, timeNumbersOverride);
    }

    public String glueDateTimeFormat(
            String date,
            String time,
            String calendar,
            String length,
            String formatType,
            ICUServiceBuilder icuServiceBuilder) {
        // calls getDateTimeFormatXpath, load the glue pattern, then call
        // glueDateTimeFormatWithGluePattern
        String xpath = this.getDateTimeFormatXpath(calendar, length, formatType);
        String gluePattern = this.getWinningValue(xpath);
        return this.glueDateTimeFormatWithGluePattern(
                date, time, calendar, gluePattern, icuServiceBuilder);
    }

    public String glueDateTimeFormatWithGluePattern(
            String date,
            String time,
            String calendar,
            String gluePattern,
            ICUServiceBuilder icuServiceBuilder) {
        // uses SimpleDateFormat to get rid of quotes
        SimpleDateFormat temp = icuServiceBuilder.getDateFormat(calendar, gluePattern, null);
        TimeZone tempTimeZone = TimeZone.GMT_ZONE;
        Calendar tempCalendar = Calendar.getInstance(tempTimeZone, ULocale.ENGLISH);
        Date tempDate = tempCalendar.getTime();
        String gluePatternWithoutQuotes = temp.format(tempDate);

        // uses MessageFormat to interpret the placeholders in the glue pattern
        return MessageFormat.format(gluePatternWithoutQuotes, (Object[]) new String[] {time, date});
    }

    public String getDateSkeleton(String calendar, String length) {
        String dateTimeSkeletonXPath = // Get standard dateTime skeleton for same calendar & length
                // as this dateTimePattern
                this.getDateSkeletonXpath(calendar, length);
        return this.getWinningValue(dateTimeSkeletonXPath);
    }

    /**
     * @see com.ibm.icu.util.Freezable#isFrozen()
     */
    @Override
    public synchronized boolean isFrozen() {
        return locked;
    }

    /**
     * @see com.ibm.icu.util.Freezable#freeze()
     */
    @Override
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

    /** Sets a final comment, replacing everything that was there. */
    public CLDRFile setFinalComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().setFinalComment(comment);
        return this;
    }

    /** Adds a comment to the final list of comments. */
    public CLDRFile appendFinalComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource
                .getXpathComments()
                .setFinalComment(
                        CldrUtility.joinWithSeparation(
                                dataSource.getXpathComments().getFinalComment(),
                                XPathParts.NEWLINE,
                                comment));
        return this;
    }

    /** Sets the initial comment, replacing everything that was there. */
    public CLDRFile setInitialComment(String comment) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().setInitialComment(comment);
        return this;
    }

    // ========== STATIC UTILITIES ==========

    /**
     * Utility to restrict to files matching a given regular expression. The expression does not
     * contain ".xml". Note that supplementalData is always skipped, and root is always included.
     */
    public static Set<String> getMatchingXMLFiles(File[] sourceDirs, Matcher m) {
        Set<String> s = new TreeSet<>();

        for (File dir : sourceDirs) {
            if (!dir.exists()) {
                throw new IllegalArgumentException("Directory doesn't exist:\t" + dir.getPath());
            }
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException(
                        "Input isn't a file directory:\t" + dir.getPath());
            }
            File[] files = dir.listFiles();
            for (File file : files) {
                String name = file.getName();
                if (!name.endsWith(".xml") || name.startsWith(".")) continue;
                // if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
                String locale = name.substring(0, name.length() - 4); // drop .xml
                if (!m.reset(locale).matches()) continue;
                s.add(locale);
            }
        }
        return s;
    }

    private final boolean DEFAULT_ITERATION_INCLUDES_EXTRAS = true;

    public Iterator<String> iterator() {
        if (DEFAULT_ITERATION_INCLUDES_EXTRAS) {
            return Iterators.filter(fullIterable().iterator(), p -> getStringValue(p) != null);
        } else {
            return iteratorWithoutExtras();
        }
    }

    public Iterator<String> iterator(String prefix) {
        if (DEFAULT_ITERATION_INCLUDES_EXTRAS) {
            if (prefix == null || prefix.isEmpty()) {
                return iterator();
            }
            return Iterators.filter(iterator(), p -> p.startsWith(prefix));
        } else {
            return iteratorWithoutExtras(prefix);
        }
    }

    public Iterator<String> iterator(Matcher pathFilter) {
        if (DEFAULT_ITERATION_INCLUDES_EXTRAS) {
            if (pathFilter == null) {
                return iterator();
            }
            return Iterators.filter(iterator(), p -> pathFilter.reset(p).matches());
        } else {
            return iteratorWithoutExtras(pathFilter);
        }
    }

    public Iterator<String> iterator(String prefix, Comparator<String> comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("iterator requires non-null comparator");
        }
        if (DEFAULT_ITERATION_INCLUDES_EXTRAS) {
            Iterator<String> it =
                    (prefix == null || prefix.isEmpty()) ? iterator() : iterator(prefix);
            Set<String> orderedSet = new TreeSet<>(comparator);
            it.forEachRemaining(orderedSet::add);
            return orderedSet.iterator();
        } else {
            return iteratorWithoutExtras(prefix, comparator);
        }
    }

    private Iterator<String> iteratorWithoutExtras() {
        return dataSource.iterator();
    }

    private synchronized Iterator<String> iteratorWithoutExtras(String prefix) {
        return dataSource.iterator(prefix);
    }

    private Iterator<String> iteratorWithoutExtras(Matcher pathFilter) {
        return dataSource.iterator(pathFilter);
    }

    public Iterator<String> iteratorWithoutExtras(String prefix, Comparator<String> comparator) {
        Iterator<String> it =
                (prefix == null || prefix.isEmpty())
                        ? dataSource.iterator()
                        : dataSource.iterator(prefix);
        if (comparator == null) return it;
        Set<String> orderedSet = new TreeSet<>(comparator);
        it.forEachRemaining(orderedSet::add);
        return orderedSet.iterator();
    }

    public Iterable<String> iterableWithoutExtras() {
        return this::iteratorWithoutExtras;
    }

    public Iterable<String> fullIterable() {
        return new FullIterable(this);
    }

    private static class FullIterable implements Iterable<String>, SimpleIterator<String> {
        private final CLDRFile file;
        private final Iterator<String> iteratorWithoutExtras;
        private Iterator<String> extraPaths;

        FullIterable(CLDRFile file) {
            this.file = file;
            this.iteratorWithoutExtras = file.iteratorWithoutExtras();
        }

        @Override
        public Iterator<String> iterator() {
            // With.toIterator relies on the next() method to turn this FullIterable into an
            // Iterator
            return With.toIterator(this);
        }

        @Override
        public String next() {
            if (iteratorWithoutExtras.hasNext()) {
                return iteratorWithoutExtras.next();
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
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        if (path1.equals(path2)) {
            return true;
        }
        // TODO: optimize
        if (!path1.contains("[@draft=") && !path2.contains("[@draft=")) {
            return false;
        }
        return getNondraftNonaltXPath(path1).equals(getNondraftNonaltXPath(path2));
    }

    /*
     * TODO: clarify the need for syncObject.
     * Formerly, an XPathParts object named "nondraftParts" was used for this purpose, but
     * there was no evident reason for it to be an XPathParts object rather than any other
     * kind of object.
     */
    private static final Object syncObject = new Object();

    public static String getNondraftNonaltXPath(String xpath) {
        if (!xpath.contains("draft=\"") && !xpath.contains("alt=\"")) {
            return xpath;
        }
        synchronized (syncObject) {
            XPathParts parts =
                    XPathParts.getFrozenInstance(xpath)
                            .cloneAsThawed(); // can't be frozen since we call removeAttributes
            String restore;
            HashSet<String> toRemove = new HashSet<>();
            for (int i = 0; i < parts.size(); ++i) {
                if (parts.getAttributeCount(i) == 0) {
                    continue;
                }
                Map<String, String> attributes = parts.getAttributes(i);
                toRemove.clear();
                restore = null;
                for (String attribute : attributes.keySet()) {
                    if (attribute.equals("draft")) {
                        toRemove.add(attribute);
                    } else if (attribute.equals("alt")) {
                        String value = attributes.get(attribute);
                        int proposedPos = value.indexOf("proposed");
                        if (proposedPos >= 0) {
                            toRemove.add(attribute);
                            if (proposedPos > 0) {
                                restore =
                                        value.substring(
                                                0, proposedPos - 1); // is of form xxx-proposedyyy
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

    /** Utility to create a validating XML reader. */
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
        for (String s : testList) {
            try {
                result =
                        (!s.isEmpty())
                                ? XMLReaderFactory.createXMLReader(s)
                                : XMLReaderFactory.createXMLReader();
                result.setFeature("http://xml.org/sax/features/validation", validating);
                break;
            } catch (SAXException e1) {
            }
        }
        if (result == null)
            throw new NoClassDefFoundError(
                    "No SAX parser is available, or unable to set validation correctly");
        return result;
    }

    /**
     * Return a directory to supplemental data used by this CLDRFile. If the CLDRFile is not
     * normally disk-based, the returned directory may be temporary and not guaranteed to exist past
     * the lifetime of the CLDRFile. The directory should be considered read-only.
     */
    public File getSupplementalDirectory() {
        if (supplementalDirectory == null) {
            // ask CLDRConfig.
            supplementalDirectory =
                    CLDRConfig.getInstance().getSupplementalDataInfo().getDirectory();
        }
        return supplementalDirectory;
    }

    public CLDRFile setSupplementalDirectory(File supplementalDirectory) {
        this.supplementalDirectory = supplementalDirectory;
        return this;
    }

    public static boolean isSupplementalName(String localeName) {
        return SUPPLEMENTAL_NAMES.contains(localeName);
    }

    private static class MyDeclHandler implements AllHandler {
        private static final UnicodeSet whitespace = new UnicodeSet("[:whitespace:]");
        private final DraftStatus minimalDraftStatus;
        private static final boolean SHOW_START_END = false;
        private int commentStack;
        private boolean justPopped = false;
        private String lastChars = "";
        // private String currentXPath = "/";
        private String currentFullXPath = "/";
        private String comment = null;
        private Map<String, String> attributeOrder;
        private DtdData dtdData;
        private final CLDRFile target;
        private String lastActiveLeafNode;
        private String lastLeafNode;
        private int isSupplemental = -1;
        private final int[] orderedCounter =
                new int[30]; // just make deep enough to handle any CLDR file.
        private final String[] orderedString =
                new String[30]; // just make deep enough to handle any CLDR file.
        private int level = 0;
        private int overrideCount = 0;
        private Locator documentLocator = null;

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
            if (!lastChars.isEmpty()) {
                if (whitespace.containsAll(lastChars)) lastChars = "";
                else
                    throw new IllegalArgumentException(
                            "Must not have mixed content: "
                                    + qName
                                    + ", "
                                    + show(attributes)
                                    + ", Content: "
                                    + lastChars);
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

                    // if (!isSupplemental) ldmlComparator.addAttribute(attribute); // must do
                    // BEFORE put
                    // ldmlComparator.addValue(value);
                    // special fix to remove version
                    // <!ATTLIST version number CDATA #REQUIRED >
                    // <!ATTLIST version cldrVersion CDATA #FIXED "24" >
                    if (attribute.equals("cldrVersion") && (qName.equals("version"))) {
                        ((SimpleXMLSource) target.dataSource)
                                .setDtdVersionInfo(VersionInfo.getInstance(value));
                    } else {
                        putAndFixDeprecatedAttribute(qName, attribute, value);
                    }
                }
                for (String attribute : attributeOrder.keySet()) {
                    String value = attributeOrder.get(attribute);
                    String both =
                            "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
                    currentFullXPath += both;
                }
            }
            if (comment != null) {
                if (currentFullXPath.equals("//ldml")
                        || currentFullXPath.equals("//supplementalData")) {
                    target.setInitialComment(comment);
                } else {
                    target.addComment(
                            currentFullXPath, comment, XPathParts.Comments.CommentType.PREBLOCK);
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
                if (value.equals("true")) value = "approved";
                else if (value.equals("false")) value = "unconfirmed";
            } else if (attribute.equals("type")) {
                if (changedTypes.contains(element)
                        && isSupplemental < 1) { // measurementSystem for example did not
                    // change from 'type' to 'choice'.
                    attribute = "choice";
                }
            }
            attributeOrder.put(attribute, value);
        }

        /** Types which changed from 'type' to 'choice', but not in supplemental data. */
        private static final Set<String> changedTypes =
                new HashSet<>(
                        Arrays.asList(
                                "abbreviationFallback",
                                "default",
                                "mapping",
                                "measurementSystem",
                                "preferenceOrdering"));

        Matcher draftMatcher = DRAFT_PATTERN.matcher("");

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
                    if (!fullXPath.startsWith("//ldml/identity/version")
                            && !fullXPath.startsWith("//ldml/identity/generation")) {
                        warnOnOverride(former, formerPath);
                    }
                }
            }
            value = trimWhitespaceSpecial(value);
            target.add(fullXPath, value)
                    .addSourceLocation(fullXPath, new XMLSource.SourceLocation(documentLocator));
        }

        private void pop(String qName) {
            Log.logln(LOG_PROGRESS, "pop\t" + qName);
            --level;

            if (!lastChars.isEmpty() || !justPopped) {
                boolean acceptItem = minimalDraftStatus == DraftStatus.unconfirmed;
                if (!acceptItem) {
                    if (draftMatcher.reset(currentFullXPath).find()) {
                        DraftStatus foundStatus = DraftStatus.valueOf(draftMatcher.group(1));
                        if (minimalDraftStatus.compareTo(foundStatus) <= 0) {
                            // what we found is greater than or equal to our status
                            acceptItem = true;
                        }
                    } else {
                        acceptItem =
                                true; // if not found, then the draft status is approved, so it is
                        // always ok
                    }
                }
                if (acceptItem) {
                    // Change any deprecated orientation attributes into values
                    // for backwards compatibility.
                    boolean skipAdd = false;
                    if (currentFullXPath.startsWith("//ldml/layout/orientation")) {
                        XPathParts parts = XPathParts.getFrozenInstance(currentFullXPath);
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
                Log.logln(
                        LOG_PROGRESS && lastActiveLeafNode != null,
                        "pop: zeroing last leafNode: " + lastActiveLeafNode);
                lastActiveLeafNode = null;
                if (comment != null) {
                    target.addComment(
                            lastLeafNode, comment, XPathParts.Comments.CommentType.POSTBLOCK);
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
            System.out.println(
                    "\tERROR in "
                            + target.getLocaleID()
                            + ";\toverriding old value <"
                            + former
                            + "> at path "
                            + distinguishing
                            + "\twith\t<"
                            + lastChars
                            + ">"
                            + CldrUtility.LINE_SEPARATOR
                            + "\told fullpath: "
                            + formerPath
                            + CldrUtility.LINE_SEPARATOR
                            + "\tnew fullpath: "
                            + currentFullXPath);
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

        @Override
        public void startElement(
                String uri, String localName, String qName, Attributes attributes) {
            Log.logln(
                    LOG_PROGRESS || SHOW_START_END,
                    "startElement uri\t"
                            + uri
                            + "\tlocalName "
                            + localName
                            + "\tqName "
                            + qName
                            + "\tattributes "
                            + show(attributes));
            try {
                if (isSupplemental < 0) { // set by first element
                    attributeOrder =
                            new TreeMap<>(
                                    // HACK for ldmlIcu
                                    dtdData.dtdType == DtdType.ldml
                                            ? CLDRFile.getAttributeOrdering()
                                            : dtdData.getAttributeComparator());
                    isSupplemental = target.dtdType == DtdType.ldml ? 0 : 1;
                }
                push(qName, attributes);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            Log.logln(
                    LOG_PROGRESS || SHOW_START_END,
                    "endElement uri\t" + uri + "\tlocalName " + localName + "\tqName " + qName);
            try {
                pop(qName);
            } catch (RuntimeException e) {
                // e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            try {
                String value = new String(ch, start, length);
                Log.logln(LOG_PROGRESS, "characters:\t" + value);
                // we will strip leading and trailing line separators in another place.
                lastChars += value;
                justPopped = false;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
            Log.logln(
                    LOG_PROGRESS,
                    "startDTD name: "
                            + name
                            + ", publicId: "
                            + publicId
                            + ", systemId: "
                            + systemId);
            commentStack++;
            target.dtdType = DtdType.fromElement(name);
            target.dtdData = dtdData = DtdData.getInstance(target.dtdType);
        }

        @Override
        public void endDTD() {
            Log.logln(LOG_PROGRESS, "endDTD");
            commentStack--;
        }

        @Override
        public void comment(char[] ch, int start, int length) {
            final String string = new String(ch, start, length);
            Log.logln(LOG_PROGRESS, commentStack + " comment " + string);
            try {
                if (commentStack != 0) return;
                String comment0 = trimWhitespaceSpecial(string).trim();
                if (lastActiveLeafNode != null) {
                    target.addComment(
                            lastActiveLeafNode, comment0, XPathParts.Comments.CommentType.LINE);
                } else {
                    comment =
                            (comment == null ? comment0 : comment + XPathParts.NEWLINE + comment0);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            if (LOG_PROGRESS)
                Log.logln(
                        LOG_PROGRESS,
                        "ignorableWhitespace length: "
                                + length
                                + ": "
                                + Utility.hex(new String(ch, start, length)));
            // if (lastActiveLeafNode != null) {
            for (int i = start; i < start + length; ++i) {
                if (ch[i] == '\n') {
                    Log.logln(
                            LOG_PROGRESS && lastActiveLeafNode != null,
                            "\\n: zeroing last leafNode: " + lastActiveLeafNode);
                    lastActiveLeafNode = null;
                    break;
                }
            }
            // }
        }

        @Override
        public void startDocument() {
            Log.logln(LOG_PROGRESS, "startDocument");
            commentStack = 0; // initialize
        }

        @Override
        public void endDocument() {
            Log.logln(LOG_PROGRESS, "endDocument");
            try {
                if (comment != null)
                    target.addComment(null, comment, XPathParts.Comments.CommentType.LINE);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        // ==== The following are just for debuggin =====

        @Override
        public void elementDecl(String name, String model) {
            Log.logln(LOG_PROGRESS, "Attribute\t" + name + "\t" + model);
        }

        @Override
        public void attributeDecl(
                String eName, String aName, String type, String mode, String value) {
            Log.logln(
                    LOG_PROGRESS,
                    "Attribute\t"
                            + eName
                            + "\t"
                            + aName
                            + "\t"
                            + type
                            + "\t"
                            + mode
                            + "\t"
                            + value);
        }

        @Override
        public void internalEntityDecl(String name, String value) {
            Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + value);
        }

        @Override
        public void externalEntityDecl(String name, String publicId, String systemId) {
            Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        @Override
        public void processingInstruction(String target, String data) {
            Log.logln(LOG_PROGRESS, "processingInstruction: " + target + ", " + data);
        }

        @Override
        public void skippedEntity(String name) {
            Log.logln(LOG_PROGRESS, "skippedEntity: " + name);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            Log.logln(LOG_PROGRESS, "setDocumentLocator Locator " + locator);
            documentLocator = locator;
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            Log.logln(LOG_PROGRESS, "startPrefixMapping prefix: " + prefix + ", uri: " + uri);
        }

        @Override
        public void endPrefixMapping(String prefix) {
            Log.logln(LOG_PROGRESS, "endPrefixMapping prefix: " + prefix);
        }

        @Override
        public void startEntity(String name) {
            Log.logln(LOG_PROGRESS, "startEntity name: " + name);
        }

        @Override
        public void endEntity(String name) {
            Log.logln(LOG_PROGRESS, "endEntity name: " + name);
        }

        @Override
        public void startCDATA() {
            Log.logln(LOG_PROGRESS, "startCDATA");
        }

        @Override
        public void endCDATA() {
            Log.logln(LOG_PROGRESS, "endCDATA");
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS || true, "error: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS, "fatalError: " + showSAX(exception));
            throw exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            Log.logln(LOG_PROGRESS, "warning: " + showSAX(exception));
            throw exception;
        }
    }

    /** Show a SAX exception in a readable form. */
    public static String showSAX(SAXParseException exception) {
        return exception.getMessage()
                + ";\t SystemID: "
                + exception.getSystemId()
                + ";\t PublicID: "
                + exception.getPublicId()
                + ";\t LineNumber: "
                + exception.getLineNumber()
                + ";\t ColumnNumber: "
                + exception.getColumnNumber();
    }

    /** Says whether the whole file is draft */
    public boolean isDraft() {
        String item = iterator().next();
        return item.startsWith("//ldml[@draft=\"unconfirmed\"]");
    }

    public Iterator<String> getAvailableIterator(NameType type) {
        String s = type.getPathStart();
        return iterator(s);
    }

    static final Relation<R2<String, String>, String> bcp47AliasMap =
            CLDRConfig.getInstance().getSupplementalDataInfo().getBcp47Aliases();

    public static String getLongTzid(String code) {
        if (!code.contains("/")) {
            Set<String> codes = bcp47AliasMap.get(Row.of("tz", code));
            if (codes != null && !codes.isEmpty()) {
                code = codes.iterator().next();
            }
        }
        return code;
    }

    /** For use in getting short names. */
    public static final Transform<String, String> SHORT_ALTS = source -> "short";

    /**
     * Get standard ordering for elements.
     *
     * @return ordered collection with items.
     * @deprecated
     */
    @Deprecated
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

    private static final Comparator<String> ldmlComparator =
            DtdData.getInstance(DtdType.ldmlICU).getDtdComparator(null);

    private static final Map<String, Map<String, String>> defaultSuppressionMap;

    static {
        String[][] data = {
            {"ldml", "version", GEN_VERSION},
            {"version", "cldrVersion", "*"},
            {"orientation", "characters", "left-to-right"},
            {"orientation", "lines", "top-to-bottom"},
            {"weekendStart", "time", "00:00"},
            {"weekendEnd", "time", "24:00"},
            {"dateFormat", "type", "standard"},
            {"timeFormat", "type", "standard"},
            {"dateTimeFormat", "type", "standard"},
            {"decimalFormat", "type", "standard"},
            {"scientificFormat", "type", "standard"},
            {"percentFormat", "type", "standard"},
            {"pattern", "type", "standard"},
            {"currency", "type", "standard"},
            {"transform", "visibility", "external"},
            {"*", "_q", "*"},
        };
        Map<String, Map<String, String>> tempmain = asMap(data, true);
        defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
    }

    public static Map<String, Map<String, String>> getDefaultSuppressionMap() {
        return defaultSuppressionMap;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map asMap(String[][] data, boolean tree) {
        Map tempmain = tree ? new TreeMap() : new HashMap();
        int len = data[0].length; // must be same for all elements
        for (int i = 0; i < data.length; ++i) {
            Map temp = tempmain;
            if (len != data[i].length) {
                throw new IllegalArgumentException("Must be square array: fails row " + i);
            }
            for (int j = 0; j < len - 2; ++j) {
                Map newTemp = (Map) temp.get(data[i][j]);
                if (newTemp == null) {
                    temp.put(data[i][j], newTemp = tree ? new TreeMap() : new HashMap());
                }
                temp = newTemp;
            }
            temp.put(data[i][len - 2], data[i][len - 1]);
        }
        return tempmain;
    }

    /** Removes a comment. */
    public CLDRFile removeComment(String string) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        dataSource.getXpathComments().removeComment(string);
        return this;
    }

    /**
     * @param draftStatus TODO
     */
    public CLDRFile makeDraft(DraftStatus draftStatus) {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        for (String path : dataSource) {
            XPathParts parts =
                    XPathParts.getFrozenInstance(dataSource.getFullPath(path))
                            .cloneAsThawed(); // not frozen, for addAttribute
            parts.addAttribute("draft", draftStatus.toString());
            dataSource.putValueAtPath(parts.toString(), dataSource.getValueAtPath(path));
        }
        return this;
    }

    static final UnicodeSet HACK_CASE_CLOSURE_SET =
            new UnicodeSet(
                            "[ſẛﬀẞ{i̇}\u1F71\u1F73\u1F75\u1F77\u1F79\u1F7B\u1F7D\u1FBB\u1FBE\u1FC9\u1FCB\u1FD3\u1FDB\u1FE3\u1FEB\u1FF9\u1FFB\u2126\u212A\u212B]")
                    .freeze();

    public UnicodeSet getExemplarSet(ExemplarType type, WinningChoice winningChoice) {
        UnicodeSet result = getRawExemplarSet(type, winningChoice);
        if (result.isEmpty()) {
            return result.cloneAsThawed();
        }
        UnicodeSet toNuke = new UnicodeSet(HACK_CASE_CLOSURE_SET).removeAll(result);
        result.closeOver(UnicodeSet.CASE_INSENSITIVE);
        result.removeAll(toNuke);
        result.remove(0x20);
        return result;
    }

    public UnicodeSet getRawExemplarSet(ExemplarType type, WinningChoice winningChoice) {
        String path = getExemplarPath(type);
        if (winningChoice == WinningChoice.WINNING) {
            path = getWinningPath(path);
        }
        String v = getStringValueWithBailey(path);
        if (v == null) {
            return UnicodeSet.EMPTY;
        }
        return SimpleUnicodeSetFormatter.parseLenient(v);
    }

    public static String getExemplarPath(ExemplarType type) {
        return "//ldml/characters/exemplarCharacters"
                + (type == ExemplarType.main ? "" : "[@type=\"" + type + "\"]");
    }

    public enum NumberingSystem {
        latin(null),
        defaultSystem("//ldml/numbers/defaultNumberingSystem"),
        nativeSystem("//ldml/numbers/otherNumberingSystems/native"),
        traditional("//ldml/numbers/otherNumberingSystems/traditional"),
        finance("//ldml/numbers/otherNumberingSystems/finance");
        public final String path;

        NumberingSystem(String path) {
            this.path = path;
        }
    }

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
            "decimal", "group", "percentSign", "perMille", "plusSign", "minusSign",
            // "infinity"
        };

        String digits = sdi.getDigits(numberingSystem);
        if (digits != null) { // TODO, get other characters, see ticket:8316
            result.addAll(digits);
        }
        for (String path : symbolPaths) {
            String fullPath =
                    "//ldml/numbers/symbols[@numberSystem=\"" + numberingSystem + "\"]/" + path;
            String value = getStringValue(fullPath);
            if (value != null) {
                result.add(value);
            }
        }

        return result;
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

    private static class DistinguishedXPath {

        public static String stats() {
            return "distinguishingMap:"
                    + distinguishingMap.size()
                    + " "
                    + "normalizedPathMap:"
                    + normalizedPathMap.size();
        }

        private static final Map<String, String> distinguishingMap = new ConcurrentHashMap<>();
        private static final Map<String, String> normalizedPathMap = new ConcurrentHashMap<>();

        static {
            distinguishingMap.put("", ""); // seed this to make the code simpler
        }

        public static String getDistinguishingXPath(String xpath, String[] normalizedPath) {
            // For example, this removes [@xml:space="preserve"] from a path with element
            // foreignSpaceReplacement.
            //     synchronized (distinguishingMap) {
            String result = distinguishingMap.get(xpath);
            if (result == null) {
                XPathParts distinguishingParts =
                        XPathParts.getFrozenInstance(xpath)
                                .cloneAsThawed(); // not frozen, for removeAttributes

                DtdType type = distinguishingParts.getDtdData().dtdType;
                Set<String> toRemove = new HashSet<>();

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
                        switch (attribute) {
                            case "draft":
                                draft = attributes.get(attribute);
                                toRemove.add(attribute);
                                break;
                            case "alt":
                                alt = attributes.get(attribute);
                                toRemove.add(attribute);
                                break;
                            case "references":
                                if (!references.isEmpty()) references += " ";
                                references += attributes.get("references");
                                toRemove.add(attribute);
                                break;
                        }
                    }
                    distinguishingParts.removeAttributes(i, toRemove);
                }
                if (draft != null || alt != null || !references.isEmpty()) {
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
                    if (!references.isEmpty()) {
                        distinguishingParts.putAttributeValue(
                                placementIndex, "references", references);
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
                normalizedPath[0] = normalizedPathMap.get(xpath);
                if (normalizedPath[0] == null) {
                    normalizedPath[0] = xpath;
                }
            }
            return result;
        }

        public Map<String, String> getNonDistinguishingAttributes(
                String fullPath, Map<String, String> result, Set<String> skipList) {
            if (result == null) {
                result = new LinkedHashMap<>();
            } else {
                result.clear();
            }
            XPathParts distinguishingParts = XPathParts.getFrozenInstance(fullPath);
            DtdType type = distinguishingParts.getDtdData().dtdType;
            for (int i = 0; i < distinguishingParts.size(); ++i) {
                String element = distinguishingParts.getElement(i);
                Map<String, String> attributes = distinguishingParts.getAttributes(i);
                for (String attribute : attributes.keySet()) {
                    if (!isDistinguishing(type, element, attribute)
                            && !skipList.contains(attribute)) {
                        result.put(attribute, attributes.get(attribute));
                    }
                }
            }
            return result;
        }
    }

    /** Fillin value for {@link CLDRFile#getSourceLocaleID(String, Status)} */
    public static class Status {
        /**
         * XPath where originally found. May be {@link GlossonymConstructor#PSEUDO_PATH} if the
         * value was constructed.
         *
         * @see GlossonymConstructor
         */
        public String pathWhereFound;

        @Override
        public String toString() {
            return pathWhereFound;
        }
    }

    public boolean isEmpty() {
        return !dataSource.iterator().hasNext();
    }

    public Map<String, String> getNonDistinguishingAttributes(
            String fullPath, Map<String, String> result, Set<String> skipList) {
        return distinguishedXPath.getNonDistinguishingAttributes(fullPath, result, skipList);
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
        return versionString == null ? null : VersionInfo.getInstance(versionString);
    }

    private boolean contains(Map<String, String> a, Map<String, String> b) {
        for (String key : b.keySet()) {
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
        XPathParts parts = XPathParts.getFrozenInstance(path);
        Map<String, String> lastAttributes = parts.getAttributes(parts.size() - 1);
        String base =
                parts.toString(parts.size() - 1)
                        + "/"
                        + parts.getElement(parts.size() - 1); // trim final element
        for (Iterator<String> it = iterator(base); it.hasNext(); ) {
            String otherPath = it.next();
            XPathParts other = XPathParts.getFrozenInstance(otherPath);
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
     * Returns the "winning" path, for use in the survey tool tests, out of all those paths that
     * only differ by having "alt proposed". The exact meaning may be tweaked over time, but the
     * user's choice (vote) has precedence, then any undisputed choice, then the "best" choice of
     * the remainders. A value is always returned if there is a valid path, and the returned value
     * is always a valid path <i>in the resolved file</i>; that is, it may be valid in the parent,
     * or valid because of aliasing.
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
     * Shortcut for getting the string value for the winning path. If the winning value is an {@link
     * CldrUtility#INHERITANCE_MARKER} (used in survey tool), then the Bailey value is returned.
     *
     * @param path
     * @return the winning value
     */
    public String getWinningValueWithBailey(String path) {
        final String winningPath = getWinningPath(path);
        return winningPath == null ? null : getStringValueWithBailey(winningPath);
    }

    /**
     * Shortcut for getting the string value for a path. If the string value is an {@link
     * CldrUtility#INHERITANCE_MARKER} (used in survey tool), then the Bailey value is returned.
     *
     * @param path
     * @return the string value
     */
    public String getStringValueWithBailey(String path) {
        return getStringValueWithBailey(path, null, null);
    }

    /**
     * Shortcut for getting the string value for a path. If the string value is an {@link
     * CldrUtility#INHERITANCE_MARKER} (used in survey tool), then the Bailey value is returned.
     *
     * @param path the given xpath
     * @param pathWhereFound if not null, to be filled in with the path where the value is actually
     *     found. May be {@link GlossonymConstructor#PSEUDO_PATH} if constructed.
     * @param localeWhereFound if not null, to be filled in with the locale where the value is
     *     actually found. May be {@link XMLSource#CODE_FALLBACK_ID} if not in root.
     * @return the string value
     */
    public String getStringValueWithBailey(
            String path, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        String value = getStringValue(path);
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            value = getBaileyValue(path, pathWhereFound, localeWhereFound);
        } else if (localeWhereFound != null || pathWhereFound != null) {
            final Status status = new Status();
            final String localeWhereFound2 = getSourceLocaleID(path, status);
            if (localeWhereFound != null) {
                localeWhereFound.value = localeWhereFound2;
            }
            if (pathWhereFound != null) {
                pathWhereFound.value = status.pathWhereFound;
            }
        }
        return value;
    }

    /**
     * Return the distinguished paths that have the specified value. The pathPrefix and pathMatcher
     * can be used to restrict the returned paths to those matching. The pathMatcher can be null
     * (equals .*).
     *
     * @param valueToMatch
     * @param pathPrefix
     * @return
     */
    public Set<String> getPathsWithValue(
            String valueToMatch, String pathPrefix, Matcher pathMatcher, Set<String> result) {
        if (result == null) {
            result = new HashSet<>();
        }
        dataSource.getPathsWithValue(valueToMatch, pathPrefix, result);
        if (pathMatcher == null) {
            return result;
        }
        result.removeIf(path -> !pathMatcher.reset(path).matches());
        return result;
    }

    /**
     * Return the distinguished paths that match the pathPrefix and pathMatcher The pathMatcher can
     * be null (equals .*).
     */
    public Set<String> getPaths(String pathPrefix, Matcher pathMatcher, Set<String> result) {
        if (result == null) {
            result = new HashSet<>();
        }
        for (Iterator<String> it = dataSource.iterator(pathPrefix); it.hasNext(); ) {
            String path = it.next();
            if (pathMatcher != null && !pathMatcher.reset(path).matches()) {
                continue;
            }
            result.add(path);
        }
        return result;
    }

    public enum WinningChoice {
        NORMAL,
        WINNING
    }

    /**
     * Used in TestUser to get the "winning" path. Simple implementation just for testing.
     *
     * @author markdavis
     */
    static class WinningComparator implements Comparator<String> {
        String user;

        public WinningComparator(String user) {
            this.user = user;
        }

        /**
         * if it contains the user, sort first. Otherwise use normal string sorting. A better
         * implementation would look at the number of votes next, and whither there was an approved
         * or provisional path.
         */
        @Override
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
     */
    public static class TestUser extends CLDRFile {

        Map<String, String> userOverrides = new HashMap<>();

        public TestUser(CLDRFile baseFile, String user, boolean resolved) {
            super(resolved ? baseFile.dataSource : baseFile.dataSource.getUnresolving());
            if (!baseFile.isResolved()) {
                throw new IllegalArgumentException("baseFile must be resolved");
            }
            Relation<String, String> pathMap =
                    Relation.of(new HashMap<>(), TreeSet.class, new WinningComparator(user));
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
        if (this.getDtdType() != DtdType.ldml) {
            return Collections.emptySet();
        }
        Set<String> toAddTo = new HashSet<>(getRawExtraPaths());
        for (String path : this.iterableWithoutExtras()) {
            toAddTo.remove(path);
        }
        return toAddTo;
    }

    /**
     * Returns the extra paths, skipping those that are already represented in the locale.
     *
     * @return
     */
    public Collection<String> getExtraPaths(String prefix, Collection<String> toAddTo) {
        if (this.getDtdType() != DtdType.ldml) {
            return Collections.emptySet();
        }
        for (String item : getRawExtraPaths()) {
            if (item.startsWith(prefix)
                    && dataSource.getValueAtPath(item) == null) { // don't use getStringValue, since
                // it recurses.
                toAddTo.add(item);
            }
        }
        return toAddTo;
    }

    // extraPaths contains the raw extra paths.
    // It requires filtering in those cases where we don't want duplicate paths.
    /**
     * Returns the raw extra paths, irrespective of what paths are already represented in the
     * locale.
     *
     * @return
     */
    public Set<String> getRawExtraPaths() {
        if (this.getDtdType() != DtdType.ldml) {
            return Collections.emptySet();
        }
        if (extraPaths == null) {
            extraPaths = ImmutableSet.<String>builder().addAll(getRawExtraPathsPrivate()).build();
            if (DEBUG) {
                System.out.println(getLocaleID() + "\textras: " + extraPaths.size());
            }
        }
        return extraPaths;
    }

    /**
     * Add (possibly over four thousand) extra paths to the given collection. These are paths that
     * typically don't have a reasonable fallback value that could be added to root. Some of them
     * are common to all locales, and some of them are specific to the given locale, based on
     * features like the plural rules for the locale.
     *
     * @return toAddTo (the collection)
     *     <p>Called only by getRawExtraPaths.
     *     <p>"Raw" refers to the fact that some of the paths may duplicate paths that are already
     *     in this CLDRFile (in the xml and/or votes), in which case they will later get filtered by
     *     getExtraPaths (removed from toAddTo) rather than re-added.
     *     <p>NOTE: values may be null for some "extra" paths in locales for which no explicit
     *     values have been submitted. Both unit tests and Survey Tool client code generate errors
     *     or warnings for null value, but allow null value for certain exceptional extra paths. See
     *     the functions named extraPathAllowsNullValue in TestPaths.java and in the JavaScript
     *     client code. Make sure that updates here are reflected there and vice versa.
     *     <p>Reference: https://unicode-org.atlassian.net/browse/CLDR-11238
     */
    private List<String> getRawExtraPathsPrivate() {
        Set<String> toAddTo = new HashSet<>();
        ExtraPaths.addConstant(toAddTo);
        ExtraPaths.addLocaleDependent(toAddTo, this.iterableWithoutExtras(), getLocaleID());
        return toAddTo.stream().map(String::intern).collect(Collectors.toList());
    }

    /**
     * Get the path with the given count, case, or gender, with fallback. The fallback acts like an
     * alias in root.
     *
     * <p>Count:
     *
     * <p>It acts like there is an alias in root from count=n to count=one, then for currency
     * display names from count=one to no count <br>
     * For unitPatterns, falls back to Count.one. <br>
     * For others, falls back to Count.one, then no count.
     *
     * <p>Case
     *
     * <p>The fallback is to no case, which = nominative.
     *
     * <p>Case
     *
     * <p>The fallback is to no case, which = nominative.
     *
     * @param xpath
     * @param count Count may be null. Returns null if nothing is found.
     * @param winning TODO
     * @return
     */
    public String getCountPathWithFallback(String xpath, Count count, boolean winning) {
        String result;
        XPathParts parts =
                XPathParts.getFrozenInstance(xpath)
                        .cloneAsThawed(); // not frozen, addAttribute in getCountPathWithFallback2

        // In theory we should do all combinations of gender, case, count (and eventually
        // definiteness), but for simplicity
        // we just successively try "zeroing" each one in a set order.
        // tryDefault modifies the parts in question
        Output<String> newPath = new Output<>();
        if (tryDefault(parts, "gender", newPath)) {
            return newPath.value;
        }

        if (tryDefault(parts, "case", newPath)) {
            return newPath.value;
        }

        boolean isDisplayName = parts.containsElement("displayName");

        String actualCount = parts.getAttributeValue(-1, "count");
        if (actualCount != null) {
            if (CldrUtility.DIGITS.containsAll(actualCount)) {
                try {
                    int item = Integer.parseInt(actualCount);
                    String locale = getLocaleID();
                    SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
                    PluralRules rules =
                            sdi.getPluralRules(
                                    new ULocale(locale), PluralRules.PluralType.CARDINAL);
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
        return null;
    }

    /**
     * Modify the parts by setting the attribute in question to the default value (typically null to
     * clear). If there is a value for that path, use it.
     */
    private boolean tryDefault(XPathParts parts, String attribute, Output<String> newPath) {
        String oldValue = parts.getAttributeValue(-1, attribute);
        if (oldValue != null) {
            parts.setAttribute(-1, attribute, null);
            newPath.value = parts.toString();
            if (dataSource.getValueAtPath(newPath.value) != null) {
                return true;
            }
        }
        return false;
    }

    private String getCountPathWithFallback2(
            XPathParts parts, String xpathWithNoCount, Count count, boolean winning) {
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
     * Returns a value to be used for "filling in" a "Change" value in the survey tool. Currently
     * returns the following.
     *
     * <ul>
     *   <li>The "winning" value (if not inherited). Example: if "Donnerstag" has the most votes for
     *       'thursday', then clicking on the empty field will fill in "Donnerstag"
     *   <li>The singular form. Example: if the value for 'hour' is "heure", then clicking on the
     *       entry field for 'hours' will insert "heure".
     *   <li>The parent's value. Example: if I'm in [de_CH] and there are no proposals for
     *       'thursday', then clicking on the empty field will fill in "Donnerstag" from [de].
     *   <li>Otherwise don't fill in anything, and return null.
     * </ul>
     *
     * @return
     */
    public String getFillInValue(String distinguishedPath) {
        String winningPath = getWinningPath(distinguishedPath);
        if (isNotRoot(winningPath)) {
            return getStringValue(winningPath);
        }
        String fallbackPath = getFallbackPath(winningPath, true, true);
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
        return source != null
                && !source.equals(LocaleNames.ROOT)
                && !source.equals(XMLSource.CODE_FALLBACK_ID);
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
        return dtdType != null ? dtdType : dataSource.getDtdType();
    }

    public DtdData getDtdData() {
        return dtdData != null ? dtdData : DtdData.getInstance(getDtdType());
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

    public void disableCaching() {
        dataSource.disableCaching();
    }

    /**
     * Get a constructed value for the given path, if it is a path for which values can be
     * constructed
     *
     * @param xpath the given path, such as
     *     //ldml/localeDisplayNames/languages/language[@type="zh_Hans"]
     * @return the constructed value, or null if this path doesn't have a constructed value
     */
    public String getConstructedValue(String xpath) {
        if (isResolved() && GlossonymConstructor.pathIsEligible(xpath)) {
            return new GlossonymConstructor(this).getValue(xpath);
        }
        return null;
    }

    /**
     * Create an overriding LocaleStringProvider for testing and example generation
     *
     * @param pathAndValueOverrides
     * @return
     */
    public LocaleStringProvider makeOverridingStringProvider(
            Map<String, String> pathAndValueOverrides) {
        return new OverridingStringProvider(pathAndValueOverrides);
    }

    public class OverridingStringProvider implements LocaleStringProvider {
        private final Map<String, String> pathAndValueOverrides;

        public OverridingStringProvider(Map<String, String> pathAndValueOverrides) {
            this.pathAndValueOverrides = pathAndValueOverrides;
        }

        @Override
        public String getStringValue(String xpath) {
            String value = pathAndValueOverrides.get(xpath);
            return value != null ? value : CLDRFile.this.getStringValue(xpath);
        }

        @Override
        public String getLocaleID() {
            return CLDRFile.this.getLocaleID();
        }

        @Override
        public String getSourceLocaleID(String xpath, Status status) {
            if (pathAndValueOverrides.containsKey(xpath)) {
                if (status != null) {
                    status.pathWhereFound = xpath;
                }
                return getLocaleID() + "-override";
            }
            return CLDRFile.this.getSourceLocaleID(xpath, status);
        }
    }

    public String getKeyName(String key) {
        String result = getStringValue("//ldml/localeDisplayNames/keys/key[@type=\"" + key + "\"]");
        if (result == null) {
            Relation<R2<String, String>, String> toAliases =
                    SupplementalDataInfo.getInstance().getBcp47Aliases();
            Set<String> aliases = toAliases.get(Row.of(key, ""));
            if (aliases != null) {
                for (String alias : aliases) {
                    result =
                            getStringValue(
                                    "//ldml/localeDisplayNames/keys/key[@type=\"" + alias + "\"]");
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public String getKeyValueName(String key, String value) {
        String result =
                getStringValue(
                        "//ldml/localeDisplayNames/types/type[@key=\""
                                + key
                                + "\"][@type=\""
                                + value
                                + "\"]");
        if (result == null) {
            Relation<R2<String, String>, String> toAliases =
                    SupplementalDataInfo.getInstance().getBcp47Aliases();
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
                        result =
                                getStringValue(
                                        "//ldml/localeDisplayNames/types/type[@key=\""
                                                + keyAlias
                                                + "\"][@type=\""
                                                + valueAlias
                                                + "\"]");
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
     * Like getStringValueWithBailey, but reject constructed values, to prevent circularity problems
     * with getName
     *
     * <p>Since GlossonymConstructor uses getName to CREATE constructed values, circularity problems
     * would occur if getName in turn used GlossonymConstructor to get constructed Bailey values.
     * Note that getStringValueWithBailey only returns a constructed value if the value would
     * otherwise be "bogus", and getName has no use for bogus values, so there is no harm in
     * returning null rather than code-fallback or other bogus values.
     *
     * @param path the given xpath
     * @return the string value, or null
     */
    String getStringValueWithBaileyNotConstructed(String path) {
        Output<String> pathWhereFound = new Output<>();
        final String value = getStringValueWithBailey(path, pathWhereFound, null);
        if (value == null || GlossonymConstructor.PSEUDO_PATH.equals(pathWhereFound.toString())) {
            return null;
        }
        return value;
    }

    // The following deprecated constants and methods are implemented only for backward
    // compatibility with https://github.com/unicode-org/unicodetools

    @Deprecated public static final int LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2;

    @Deprecated
    public String getName(int type, String code) {
        switch (type) {
            case LANGUAGE_NAME:
                return nameGetter.getNameFromTypeEnumCode(NameType.LANGUAGE, code);
            case SCRIPT_NAME:
                return nameGetter.getNameFromTypeEnumCode(NameType.SCRIPT, code);
            case TERRITORY_NAME:
                return nameGetter.getNameFromTypeEnumCode(NameType.TERRITORY, code);
            default:
                throw new IllegalArgumentException("Unrecognized type");
        }
    }

    @Deprecated
    public String getName(String code) {
        return nameGetter.getNameFromIdentifier(code);
    }

    @Deprecated
    public String getName(String type, String code) {
        if (!type.equals("territory")) {
            throw new IllegalArgumentException("First argument should be territory");
        }
        return nameGetter.getNameFromTypeEnumCode(NameType.TERRITORY, code);
    }
}
