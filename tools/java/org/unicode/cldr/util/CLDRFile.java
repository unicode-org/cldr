/*
 **********************************************************************
 * Copyright (c) 2002-2012, International Business Machines
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.With.SimpleIterator;
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

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
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

    private static final boolean DEBUG = false;

    public static final Pattern ALT_PROPOSED_PATTERN = Pattern.compile(".*\\[@alt=\"[^\"]*proposed[^\"]*\"].*");

    static Pattern FIRST_ELEMENT = Pattern.compile("//([^/\\[]*)");

    public enum DtdType {
        ldml, supplementalData, ldmlBCP47, keyboard, platform;
        public static DtdType fromPath(String elementOrPath) {
            Matcher m = FIRST_ELEMENT.matcher(elementOrPath);
            m.lookingAt();
            return DtdType.valueOf(m.group(1));
        }
    }

    private static boolean LOG_PROGRESS = false;

    public static boolean HACK_ORDER = false;
    private static boolean DEBUG_LOGGING = false;

    public static final String SUPPLEMENTAL_NAME = "supplementalData";
    public static final String SUPPLEMENTAL_METADATA = "supplementalMetadata";
    public static final String SUPPLEMENTAL_PREFIX = "supplemental";
    public static final String GEN_VERSION = "23";

    private Collection<String> extraPaths = null;

    private boolean locked;
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

    public static CLDRFile loadFromFile(File f, String localeName, DraftStatus minimalDraftStatus, XMLSource source) {
        String fullFileName = f.getAbsolutePath();
        try {
            fullFileName = f.getCanonicalPath();
            if (DEBUG_LOGGING) {
                System.out.println("Parsing: " + fullFileName);
                Log.logln(LOG_PROGRESS, "Parsing: " + fullFileName);
            }
            FileInputStream fis = new FileInputStream(f);
            CLDRFile cldrFile = load(fullFileName, localeName, fis, minimalDraftStatus, source);
            fis.close();
            return cldrFile;
        } catch (Exception e) {
            // e.printStackTrace();
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + fullFileName + " - "
                + e.toString()).initCause(e);
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

    static CLDRFile load(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
        return load(fileName, localeName, fis, minimalDraftStatus, new SimpleXMLSource(localeName));
    }

    /**
     * Load a CLDRFile from a file input stream.
     * 
     * @param localeName
     * @param fis
     */
    private static CLDRFile load(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus,
        XMLSource source) {
        try {
            fis = new StripUTF8BOMInputStream(fis);
            CLDRFile cldrFile = new CLDRFile(source);
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
                    + "; The exact problems are printed on the consol above.");
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
            throw (IllegalArgumentException) new IllegalArgumentException("Can't read " + localeName).initCause(e);
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
     * Will update the identity element, including generation, version, and other items.
     * If the CLDRFile is empty, the DTD type will be //ldml.
     */
    public CLDRFile write(PrintWriter pw) {
        return write(pw, nullOptions);
    }

    /**
     * Write the corresponding XML file out, with the normal formatting and indentation.
     * Will update the identity element, including generation, version, and other items.
     * If the CLDRFile is empty, the DTD type will be //ldml.
     * 
     * @param pw
     *            writer to print to
     * @param options
     *            map of options for writing
     */
    public CLDRFile write(PrintWriter pw, Map<String, Object> options) {
        Set<String> orderedSet = new TreeSet<String>(ldmlComparator);
        CollectionUtilities.addAll(dataSource.iterator(), orderedSet);

        String firstPath = null;
        String firstFullPath = null;
        XPathParts parts = new XPathParts(null, null);
        DtdType dtdType = DtdType.ldml; // default

        if (orderedSet.size() > 0) { // May not have any elements.
            firstPath = (String) orderedSet.iterator().next();
            // Value firstValue = (Value) getXpath_value().get(firstPath);
            firstFullPath = getFullXPath(firstPath);
            parts.set(firstFullPath);
            dtdType = DtdType.valueOf(parts.getElement(0));
        }

        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        if (!options.containsKey("DTD_OMIT")) {
            String dtdDir = "../../common/dtd/";
            if (options.containsKey("DTD_DIR")) {
                dtdDir = options.get("DTD_DIR").toString();
            }
            pw.println("<!DOCTYPE " + dtdType + " SYSTEM \"" + dtdDir + dtdType + ".dtd\">");
        }

        if (options.containsKey("COMMENT")) {
            pw.println("<!-- " + options.get("COMMENT") + " -->");
        }
        /*
         * <identity>
         * <version number="1.2"/>
         * <generation date="2004-08-27"/>
         * <language type="en"/>
         */
        // if ldml has any attributes, get them.
        Set<String> identitySet = new TreeSet<String>(ldmlComparator);
        if (isNonInheriting()) {
            // identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/version[@number=\"$" +
            // "Revision: $\"]");
            // identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/generation[@date=\"$" +
            // "Date: $\"]");
        } else {
            String ldml_identity = "//ldml/identity";
            if (firstFullPath != null) { // if we had a path
                if (firstFullPath.indexOf("/identity") >= 0) {
                    ldml_identity = parts.toString(2);
                } else {
                    ldml_identity = parts.toString(1) + "/identity";
                }
            }

            identitySet.add(ldml_identity + "/version[@number=\"$" + "Revision: $\"]");
            identitySet.add(ldml_identity + "/generation[@date=\"$" + "Date: $\"]");
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
        final String COPYRIGHT_STRING = "Copyright \u00A9 1991-"
            + Calendar.getInstance().get(Calendar.YEAR)
            + " Unicode, Inc.\n"
            + "CLDR data files are interpreted according to the LDML specification "
            + "(http://unicode.org/reports/tr35/)\n"
            + "For terms of use, see http://www.unicode.org/copyright.html";

        String initialComment = dataSource.getXpathComments().getInitialComment();
        if (!initialComment.contains("Copyright") || !initialComment.contains("Unicode")) {
            initialComment = initialComment + COPYRIGHT_STRING;
        }
        XPathParts.writeComment(pw, 0, initialComment, true);

        XPathParts.Comments tempComments = (XPathParts.Comments) dataSource.getXpathComments().clone();
        tempComments.fixLineEndings();

        MapComparator<String> modAttComp = attributeOrdering;
        if (HACK_ORDER) modAttComp = new MapComparator<String>()
            .add("alt").add("draft").add(modAttComp.getOrder());

        XPathParts last = new XPathParts(attributeOrdering, defaultSuppressionMap);
        XPathParts current = new XPathParts(attributeOrdering, defaultSuppressionMap);
        XPathParts lastFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
        XPathParts currentFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
        boolean isResolved = dataSource.isResolving();

        for (Iterator<String> it2 = identitySet.iterator(); it2.hasNext();) {
            String xpath = (String) it2.next();
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            currentFiltered.set(xpath);
            current.set(xpath);
            current.writeDifference(pw, currentFiltered, last, lastFiltered, "", tempComments);
            // exchange pairs of parts
            XPathParts temp = current;
            current = last;
            last = temp;
            temp = currentFiltered;
            currentFiltered = lastFiltered;
            lastFiltered = temp;
        }

        for (Iterator<String> it2 = orderedSet.iterator(); it2.hasNext();) {
            String xpath = (String) it2.next();
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            // Value v = (Value) getXpath_value().get(xpath);
            currentFiltered.set(xpath);
            if (currentFiltered.getElement(1).equals("identity")) continue;
            current.set(getFullXPath(xpath));
            current.writeDifference(pw, currentFiltered, last, lastFiltered, getStringValue(xpath), tempComments);
            // exchange pairs of parts
            XPathParts temp = current;
            current = last;
            last = temp;
            temp = currentFiltered;
            currentFiltered = lastFiltered;
            lastFiltered = temp;
        }
        current.clear().writeDifference(pw, null, last, lastFiltered, null, tempComments);
        String finalComment = dataSource.getXpathComments().getFinalComment();

        // write comments that no longer have a base
        List<String> x = tempComments.extractCommentsWithoutBase();
        if (x.size() != 0) {
            String extras = "Comments without bases" + XPathParts.NEWLINE;
            for (Iterator it = x.iterator(); it.hasNext();) {
                String key = (String) it.next();
                // Log.logln("Writing extra comment: " + key);
                extras += XPathParts.NEWLINE + key;
            }
            finalComment += XPathParts.NEWLINE + extras;
        }
        XPathParts.writeComment(pw, 0, finalComment, true);
        return this;
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
        // || xpath.contains("/currency") && xpath.contains("/displayName")
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
     * Find out where the value was found (for resolving locales). Returns code-fallback as the location if nothing is
     * found
     * 
     * @param distinguishedXPath
     *            path (must be distinguished!)
     * @param status
     *            the distinguished path where the item was found. Pass in null if you don't care.
     */
    public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
        String result = dataSource.getSourceLocaleID(distinguishedXPath, status);
        if (result == XMLSource.CODE_FALLBACK_ID && dataSource.isResolving()) {
            final String fallbackPath = getFallbackPath(distinguishedXPath, false);
            if (fallbackPath != null && !fallbackPath.equals(distinguishedXPath)) {
                result = dataSource.getSourceLocaleID(fallbackPath, status);
                // if (status != null && status.pathWhereFound.equals(distinguishedXPath)) {
                // status.pathWhereFound = fallbackPath;
                // }
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
            xpath = getDistinguishingXPath(xpath, null, false);
            dataSource.getXpathComments().addComment(type, xpath, comment);
        }
        return this;
    }

    // TODO Change into enum, update docs
    static final public int
        MERGE_KEEP_MINE = 0,
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

        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        XPathParts parts = new XPathParts(null, null);
        if (conflict_resolution == MERGE_KEEP_MINE) {
            Map temp = isNonInheriting() ? new TreeMap() : new TreeMap(ldmlComparator);
            dataSource.putAll(other.dataSource, MERGE_KEEP_MINE);
        } else if (conflict_resolution == MERGE_REPLACE_MINE) {
            dataSource.putAll(other.dataSource, MERGE_REPLACE_MINE);
        } else if (conflict_resolution == MERGE_REPLACE_MY_DRAFT) {
            // first find all my alt=..proposed items
            Set hasDraftVersion = new HashSet();
            for (Iterator it = dataSource.iterator(); it.hasNext();) {
                String cpath = (String) it.next();
                String fullpath = getFullXPath(cpath);
                if (fullpath.indexOf("[@draft") >= 0) {
                    hasDraftVersion.add(getNondraftNonaltXPath(cpath)); // strips the alt and the draft
                }
            }
            // only replace draft items!
            // this is either an item with draft in the fullpath
            // or an item with draft and alt in the full path
            for (Iterator it = other.iterator(); it.hasNext();) {
                String cpath = (String) it.next();
                // Value otherValueOld = (Value) other.getXpath_value().get(cpath);
                // fix the data
                // cpath = Utility.replace(cpath, "[@type=\"ZZ\"]", "[@type=\"QO\"]"); // fix because tag meaning
                // changed after beta
                cpath = getNondraftNonaltXPath(cpath);
                String newValue = other.getStringValue(cpath);
                String newFullPath = getNondraftNonaltXPath(other.getFullXPath(cpath));
                // newFullPath = Utility.replace(newFullPath, "[@type=\"ZZ\"]", "[@type=\"QO\"]");
                // another hack; need to add references back in
                newFullPath = addReferencesIfNeeded(newFullPath, getFullXPath(cpath));
                // Value otherValue = new StringValue(newValue, newFullPath);

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
            for (Iterator it = other.iterator(); it.hasNext();) {
                String key = (String) it.next();
                String otherValue = other.getStringValue(key);
                String myValue = dataSource.getValueAtPath(key);
                if (myValue == null) {
                    dataSource.putValueAtPath(other.getFullXPath(key), otherValue);
                } else if (!(myValue.equals(otherValue)
                    && equalsIgnoringDraft(getFullXPath(key), other.getFullXPath(key)))
                    && !key.startsWith("//ldml/identity")) {
                    for (int i = 0;; ++i) {
                        String prop = "proposed" + (i == 0 ? "" : String.valueOf(i));
                        String fullPath = parts.set(other.getFullXPath(key)).addAttribute("alt", prop).toString();
                        String path = getDistinguishingXPath(fullPath, null, false);
                        if (dataSource.getValueAtPath(path) != null) continue;
                        dataSource.putValueAtPath(fullPath, otherValue);
                        break;
                    }
                }
            }
        } else
            throw new IllegalArgumentException("Illegal operand: " + conflict_resolution);

        dataSource.getXpathComments().setInitialComment(
            CldrUtility.joinWithSeparation(dataSource.getXpathComments().getInitialComment(),
                XPathParts.NEWLINE,
                other.dataSource.getXpathComments().getInitialComment()));
        dataSource.getXpathComments().setFinalComment(
            CldrUtility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(),
                XPathParts.NEWLINE,
                other.dataSource.getXpathComments().getFinalComment()));
        dataSource.getXpathComments().joinAll(other.dataSource.getXpathComments());
        /*
         * private Map xpath_value;
         * private String initialComment = "";
         * private String finalComment = "";
         * private String key;
         * private XPathParts.Comments xpath_comments = new XPathParts.Comments(); // map from paths to comments.
         * private boolean isSupplemental;
         */
        return this;
    }

    /**
   * 
   */
    private String addReferencesIfNeeded(String newFullPath, String fullXPath) {
        if (fullXPath == null || fullXPath.indexOf("[@references=") < 0) return newFullPath;
        XPathParts parts = new XPathParts(null, null).set(fullXPath);
        String accummulatedReferences = null;
        for (int i = 0; i < parts.size(); ++i) {
            Map attributes = parts.getAttributes(i);
            String references = (String) attributes.get("references");
            if (references == null) continue;
            if (accummulatedReferences == null)
                accummulatedReferences = references;
            else
                accummulatedReferences += ", " + references;
        }
        if (accummulatedReferences == null) return newFullPath;
        XPathParts newParts = new XPathParts(null, null).set(newFullPath);
        Map attributes = newParts.getAttributes(newParts.size() - 1);
        String references = (String) attributes.get("references");
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
            // CLDRFile.Value v = getValue(xpath);
            appendFinalComment(dataSource.getFullPath(xpath) + "::<" + dataSource.getValueAtPath(xpath) + ">");
        }
        dataSource.removeValueAtPath(xpath);
        return this;
    }

    /**
     * Removes all xpaths from a CLDRFile.
     */
    public CLDRFile removeAll(Set xpaths, boolean butComment) {
        if (butComment) appendFinalComment("Illegal attributes removed:");
        for (Iterator it = xpaths.iterator(); it.hasNext();) {
            remove((String) it.next(), butComment);
        }
        return this;
    }

    /**
     * Code should explicitly include CODE_FALLBACK
     */
    public static final Pattern specialsToKeep = Pattern.compile(
        "/(" +
            "measurementSystemName" +
            "|codePattern" +
            "|calendar\\[\\@type\\=\"[^\"]*\"\\]/(?!dateTimeFormats/appendItems)" + // gregorian
            "|numbers/symbols/(decimal/group)" +
            "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
            "|pattern" +
            ")");

    static public final Pattern specialsToPushFromRoot = Pattern.compile(
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
        for (Iterator it = iterator(); it.hasNext();) { // see what items we have that the other also has
            String xpath = (String) it.next();
            switch (keepIfMatches.getRetention(xpath)) {
            case RETAIN:
                continue;
            case RETAIN_IF_DIFFERENT:
                String currentValue = dataSource.getValueAtPath(xpath);
                // if (currentValue == null) continue;
                String otherXpath = xpath;
                String otherValue = other.dataSource.getValueAtPath(otherXpath);
                if (!currentValue.equals(otherValue)) {
                    if (MINIMIZE_ALT_PROPOSED) {
                        otherXpath = CLDRFile.getNondraftNonaltXPath(xpath);
                        if (otherXpath.equals(xpath)) {
                            continue;
                        }
                        otherValue = other.dataSource.getValueAtPath(otherXpath);
                        if (!currentValue.equals(otherValue)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                String keepValue = (String) XMLSource.getPathsAllowingDuplicates().get(xpath);
                if (keepValue != null && keepValue.equals(currentValue)) {
                    continue;
                }
                // we've now established that the values are the same
                String currentFullXPath = dataSource.getFullPath(xpath);
                String otherFullXPath = other.dataSource.getFullPath(otherXpath);
                if (!equalsIgnoringDraft(currentFullXPath, otherFullXPath)) {
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

            if (first) {
                first = false;
                if (butComment) appendFinalComment("Duplicates removed:");
            }
            // we can't remove right away, since that disturbs the iterator.
            removedItems.add(xpath);
            // remove(xpath, butComment);
        }
        // now remove them safely
        for (String xpath : removedItems) {
            remove(xpath, butComment);
        }
        return this;
    }

    public CLDRFile putRoot(CLDRFile rootFile) {
        Matcher specialPathMatcher = specialsToPushFromRoot.matcher("");
        XPathParts parts = new XPathParts(attributeOrdering, defaultSuppressionMap);
        for (Iterator it = rootFile.iterator(); it.hasNext();) {
            String xpath = (String) it.next();

            // skip aliases, choices
            if (xpath.contains("/alias")) continue;
            if (xpath.contains("/default")) continue;

            // skip values we have
            String currentValue = dataSource.getValueAtPath(xpath);
            if (currentValue != null) continue;

            // only copy specials
            if (!specialPathMatcher.reset(xpath).find()) { // skip certain xpaths
                continue;
            }
            // now add the value
            String otherValue = rootFile.dataSource.getValueAtPath(xpath);
            String otherFullXPath = rootFile.dataSource.getFullPath(xpath);
            if (!otherFullXPath.contains("[@draft")) {
                parts.set(otherFullXPath);
                Map attributes = parts.getAttributes(-1);
                attributes.put("draft", "unconfirmed");
                otherFullXPath = parts.toString();
            }

            add(otherFullXPath, otherValue);
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
        // Map<String,String> parts = new HashMap<String,String>();
        XPathParts xpp = new XPathParts(null, null);
        ULocale.Builder lb = new ULocale.Builder();
        for (Iterator<String> i = iterator("//ldml/identity/"); i.hasNext();) {
            xpp.set(i.next());
            String k = xpp.getElement(-1);
            String v = xpp.getAttributeValue(-1, "type");
            // parts.put(k,v);
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
                if (!name.endsWith(".xml")) continue;
                // if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
                String locale = name.substring(0, name.length() - 4); // drop .xml
                if (!m.reset(locale).matches()) continue;
                s.add(locale);
            }
        }
        return s;
    }

    /**
     * Returns a collection containing the keys for this file.
     */
    // public Set keySet() {
    // return (Set) CollectionUtilities.addAll(dataSource.iterator(), new HashSet());
    // }

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

    public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
        return distinguishedXPath.getDistinguishingXPath(xpath, normalizedPath, nonInheriting);
    }

    private static boolean equalsIgnoringDraft(String path1, String path2) {
        if (path1 == path2) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        // TODO: optimize
        if (path1.indexOf("[@draft=") < 0 && path2.indexOf("[@draft=") < 0) return path1.equals(path2);
        return getNondraftNonaltXPath(path1).equals(getNondraftNonaltXPath(path2));
    }

    static XPathParts nondraftParts = new XPathParts(null, null);

    public static String getNondraftNonaltXPath(String xpath) {
        if (xpath.indexOf("draft=\"") < 0 && xpath.indexOf("alt=\"") < 0) return xpath;
        synchronized (nondraftParts) {
            XPathParts parts = new XPathParts(null, null).set(xpath);
            String restore;
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                Map attributes = parts.getAttributes(i);
                restore = null;
                for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
                    String attribute = (String) it.next();
                    if (attribute.equals("draft"))
                        it.remove();
                    else if (attribute.equals("alt")) {
                        String value = (String) attributes.get(attribute);
                        int proposedPos = value.indexOf("proposed");
                        if (proposedPos >= 0) {
                            it.remove();
                            if (proposedPos > 0) {
                                restore = value.substring(0, proposedPos - 1); // is of form xxx-proposedyyy
                            }
                        }
                    }
                }
                if (restore != null) attributes.put("alt", restore);
            }
            return parts.toString();
        }
    }

    // private static String getNondraftXPath(String xpath) {
    // if (xpath.indexOf("draft=\"") < 0) return xpath;
    // synchronized (nondraftParts) {
    // XPathParts parts = new XPathParts(null,null).set(xpath);
    // for (int i = 0; i < parts.size(); ++i) {
    // Map attributes = parts.getAttributes(i);
    // for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
    // String attribute = (String) it.next();
    // if (attribute.equals("draft")) it.remove();
    // }
    // }
    // return parts.toString();
    // }
    // }

    static String[][] distinguishingData = {
        { "*", "key" },
        { "*", "id" },
        { "*", "_q" },
        { "*", "alt" },
        { "*", "iso4217" },
        { "*", "iso3166" },
        { "*", "indexSource" },
        { "default", "type" },
        { "measurementSystem", "type" },
        { "mapping", "type" },
        { "abbreviationFallback", "type" },
        { "preferenceOrdering", "type" },
        { "deprecatedItems", "iso3166" },
        { "ruleset", "type" },
        { "rbnfrule", "value" },
    };

    private final static Map distinguishingAttributeMap = asMap(distinguishingData, true);

    /**
     * Determine if an attribute is a distinguishing attribute.
     * 
     * @param elementName
     * @param attribute
     * @return
     */
    public static boolean isDistinguishing(DtdType type, String elementName, String attribute) {
        switch (type) {
        case ldml:
            return attribute.equals("_q")
                || attribute.equals("key")
                || attribute.equals("indexSource")
                || attribute.equals("request")
                || attribute.equals("count")
                || attribute.equals("id")
                || attribute.equals("registry")
                || attribute.equals("alt")
                || attribute.equals("mzone")
                || attribute.equals("from")
                || attribute.equals("to")
                || attribute.equals("value")
                || attribute.equals("yeartype")
                || attribute.equals("numberSystem")
                || attribute.equals("parent")
                || (attribute.equals("type")
                    && !elementName.equals("listPattern")
                    && !elementName.equals("default")
                    && !elementName.equals("measurementSystem")
                    && !elementName.equals("mapping")
                    && !elementName.equals("abbreviationFallback")
                    && !elementName.equals("preferenceOrdering"));
        case ldmlBCP47:
            return attribute.equals("_q")
                || attribute.equals("alias")
                || attribute.equals("name");
        case supplementalData:
            return attribute.equals("_q")
                || attribute.equals("iso4217")
                || attribute.equals("iso3166")
                || attribute.equals("code")
                || elementName.equals("deprecatedItems")
                && (attribute.equals("type") || attribute.equals("elements") || attribute.equals("attributes") || attribute
                    .equals("values"))
                || elementName.equals("dayPeriodRules") && attribute.equals("locales")
                || elementName.equals("dayPeriodRule") && attribute.equals("type")
                || elementName.equals("metazones") && attribute.equals("type")
                || elementName.equals("mapZone") && (attribute.equals("other") || attribute.equals("territory"))
                || elementName.equals("postCodeRegex") && attribute.equals("territoryId")
                || elementName.equals("calendarPreference") && attribute.equals("territories")
                || elementName.equals("firstDay") && attribute.equals("territories")
                || elementName.equals("weekendStart") && attribute.equals("territories")
                || elementName.equals("weekendEnd") && attribute.equals("territories")
                || elementName.equals("measurementSystem") && attribute.equals("territories")
                || elementName.equals("distinguishingItems") && attribute.equals("attributes")
                || elementName.equals("codesByTerritory") && attribute.equals("territory")
                || elementName.equals("currency") &&
                (attribute.equals("iso4217") || attribute.equals("tender"))
                || elementName.equals("territoryAlias") &&
                (attribute.equals("replacement") || attribute.equals("type"))
                || elementName.equals("territoryCodes") &&
                (attribute.equals("alpha3") || attribute.equals("numeric") || attribute.equals("type"))
                || elementName.equals("group") && attribute.equals("status")
                || elementName.equals("plurals") && attribute.equals("type")
                || elementName.equals("hours") && attribute.equals("regions");
        case keyboard:
        case platform:
            return false;
        }
        // if (result != matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true)) {
        // matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true);
        // throw new IllegalArgumentException("Failed: " + elementName + ", " + attribute);
        // }
        throw new IllegalArgumentException("Type is wrong: " + type);
    }

    public static boolean isDistinguishing(String elementName, String attribute) {
        if (isDistinguishing(DtdType.ldml, elementName, attribute)) return true;
        if (isDistinguishing(DtdType.supplementalData, elementName, attribute)) return true;
        if (isDistinguishing(DtdType.ldmlBCP47, elementName, attribute)) return true;
        return false;
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
            supplementalDirectory = new File(CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY);
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
            public boolean accept(File dir, String name) {
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
        return localeName.startsWith(SUPPLEMENTAL_PREFIX) || localeName.equals("characters");
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
        private Map attributeOrder = new TreeMap(attributeOrdering);
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
            // attributeOrder = new TreeMap(attributeOrdering);
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
            if (orderedElements.contains(qName)) {
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
                    // <!ATTLIST version cldrVersion CDATA #FIXED "23" >
                    if (attribute.equals("cldrVersion")
                        && (qName.equals("version"))) {
                        ((SimpleXMLSource) target.dataSource).setDtdVersionInfo(VersionInfo.getInstance(value));
                    } else {
                        putAndFixDeprecatedAttribute(qName, attribute, value);
                    }
                }
                for (Iterator it = attributeOrder.keySet().iterator(); it.hasNext();) {
                    String attribute = (String) it.next();
                    String value = (String) attributeOrder.get(attribute);
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

        private Set<String> fixedSkeletons = new HashSet();

        private DateTimePatternGenerator dateGenerator = DateTimePatternGenerator.getEmptyInstance();

        /**
         * Types which changed from 'type' to 'choice', but not in supplemental data.
         */
        private static Set changedTypes = new HashSet(Arrays.asList(new String[] {
            "abbreviationFallback",
            "default", "mapping", "measurementSystem", "preferenceOrdering" }));

        static final Pattern draftPattern = Pattern.compile("\\[@draft=\"([^\"]*)\"\\]");
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
                    warnOnOverride(former, formerPath);
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
                        XPathParts parts = new XPathParts().set(currentFullXPath);
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

        static Pattern WHITESPACE_WITH_LF = Pattern.compile("\\s*\\u000a\\s*");
        Matcher whitespaceWithLf = WHITESPACE_WITH_LF.matcher("");

        /**
         * Trim leading whitespace if there is a linefeed among them, then the same with trailing.
         * 
         * @param source
         * @return
         */
        private String trimWhitespaceSpecial(String source) {
            if (!source.contains("\n")) {
                return source;
            }
            source = whitespaceWithLf.reset(source).replaceAll("\n");
            return source;
            // int start = source.startsWith("\\u000a") ? 1 : 0;
            // int end = source.endsWith("\\u000a") ? source.length() - 1 : source.length();
            // return source.substring(start, end);
        }

        private void warnOnOverride(String former, String formerPath) {
            String distinguishing = CLDRFile.getDistinguishingXPath(formerPath, null, true);
            System.out.println("\tWARNING! in " + target.getLocaleID() + ";\toverriding old value <" + former
                + "> at path " + distinguishing +
                CldrUtility.LINE_SEPARATOR + "\twith\t<" + lastChars + ">;\told fullpath: " + formerPath +
                CldrUtility.LINE_SEPARATOR + "new fullpath: " + currentFullXPath);
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
            for (int i = input.length() - 1; i >= 0; --i) {
                char ch = input.charAt(i);
                switch (ch) {
                case '/':
                    if (braceStack == 0) return i;
                    break;
                case '[':
                    --braceStack;
                    break;
                case ']':
                    ++braceStack;
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
                + "\tattributes " + show(attributes)
                );
            try {
                if (isSupplemental < 0) { // set by first element
                    if (qName.equals("ldml"))
                        isSupplemental = 0;
                    else if (qName.equals("supplementalData"))
                        isSupplemental = 1;
                    else if (qName.equals("ldmlBCP47"))
                        isSupplemental = 1;
                    else
                        throw new IllegalArgumentException("File is neither ldml or supplementalData!");
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

        static final char XML_LINESEPARATOR = (char) 0xA;
        static final String XML_LINESEPARATOR_STRING = String.valueOf(XML_LINESEPARATOR);

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
                + ", systemId: " + systemId
                );
            commentStack++;
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
    // return keySet(Pattern.compile(regexPattern).matcher(""), output);
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
        LIMIT_TYPES = 13;

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
        return NameTable[type][0] + code + NameTable[type][1];
    }

    /**
     * @return the code used to access data of a given type from the path. Null if not found.
     */
    public static String getCode(String path) {
        int type = getNameType(path);
        if (type < 0) {
            throw new IllegalArgumentException("Illegal type in path: " + path);
        }
        int start = NameTable[type][0].length();
        int end = path.indexOf(NameTable[type][1], start);
        return path.substring(start, end);
    }

    /**
     * Utility for getting the name, given a code.
     * 
     * @param type
     * @param code
     * @return
     */
    public String getName(int type, String code) {
        String path = getKey(type, code);
        String result = getStringValue(path);
        if (result == null && getLocaleID().equals("en")) {
            if (type == LANGUAGE_NAME) {
                Set<String> set = Iso639Data.getNames(code);
                if (set != null) {
                    return set.iterator().next();
                }
            }
        }
        return result;
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
            if (type.equalsIgnoreCase(getNameName(i))) return i;
        }
        return -1;
    }

    transient LanguageTagParser lparser = new LanguageTagParser();

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

    public synchronized String getName(String localeOrTZID, boolean onlyConstructCompound) {
        boolean isCompound = localeOrTZID.contains("_");
        String name = isCompound && onlyConstructCompound ? null : getName(LANGUAGE_NAME, localeOrTZID);
        // TODO - handle arbitrary combinations
        if (name != null && !name.contains("_") && !name.contains("-")) {
            return name;
        }
        lparser.set(localeOrTZID);
        String original;

        // we need to check for prefixes, for lang+script or lang+country
        boolean haveScript = false;
        boolean haveRegion = false;
        // try lang+script
        if (onlyConstructCompound) {
            name = getName(LANGUAGE_NAME, original = lparser.getLanguage());
            if (name == null) name = original;
        } else {
            name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT_REGION));
            if (name != null) {
                haveScript = haveRegion = true;
            } else {
                name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT));
                if (name != null) {
                    haveScript = true;
                } else {
                    name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_REGION));
                    if (name != null) {
                        haveRegion = true;
                    } else {
                        name = getName(LANGUAGE_NAME, original = lparser.getLanguage());
                        if (name == null) name = original;
                    }
                }
            }
        }

        String nameSeparator = getWinningValue("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator");
        String sname;
        String extras = "";
        if (!haveScript) {
            sname = original = lparser.getScript();
            if (sname.length() != 0) {
                if (extras.length() != 0) extras += nameSeparator;
                sname = getName(SCRIPT_NAME, sname);
                extras += (sname == null ? original : sname);
            }
        }
        if (!haveRegion) {
            original = sname = lparser.getRegion();
            if (sname.length() != 0) {
                if (extras.length() != 0) extras += nameSeparator;
                sname = getName(TERRITORY_NAME, sname);
                extras += (sname == null ? original : sname);
            }
        }
        List<String> variants = lparser.getVariants();
        for (int i = 0; i < variants.size(); ++i) {
            if (extras.length() != 0) extras += nameSeparator;
            sname = getName(VARIANT_NAME, original = (String) variants.get(i));
            extras += (sname == null ? original : sname);
        }
        // Look for key-type pairs.
        for (Entry<String, String> extension : lparser.getLocaleExtensions().entrySet()) {
            String key = extension.getKey();
            String type = extension.getValue();
            // Check if key/type pairs exist in the CLDRFile first.
            String valuePath = "//ldml/localeDisplayNames/types/type[@type=\"" + type + "\"][@key=\"" + key + "\"]";
            String value = null;
            // Ignore any values from code-fallback.
            if (!getSourceLocaleID(valuePath, null).equals(XMLSource.CODE_FALLBACK_ID)) {
                value = getStringValue(valuePath);
            }
            if (value == null) {
                // Get name of key instead and pair it with the type as-is.
                String keyTypePattern = getWinningValue("//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern");
                sname = getStringValue("//ldml/localeDisplayNames/keys/key[@type=\"" + key + "\"]");
                if (sname == null) sname = key;
                value = MessageFormat.format(keyTypePattern, new Object[] { sname, type });
            }
            extras += ", " + value;
        }
        // fix this -- shouldn't be hardcoded!
        if (extras.length() == 0) {
            return name;
        }
        String namePattern = getWinningValue("//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
        return MessageFormat.format(namePattern, new Object[] { name, extras });
    }

    /**
     * Returns the name of a type.
     */
    public static String getNameName(int choice) {
        return NameTable[choice][2];
    }

    /**
     * Get standard ordering for elements.
     * 
     * @return ordered collection with items.
     */
    public static List<String> getElementOrder() {
        return elementOrdering.getOrder(); // already unmodifiable
    }

    /**
     * Get standard ordering for attributes.
     * 
     * @return ordered collection with items.
     */
    public static List<String> getAttributeOrder() {
        return attributeOrdering.getOrder(); // already unmodifiable
    }

    /**
     * Get standard ordering for attributes.
     * 
     * @return ordered collection with items.
     */
    public static Comparator<String> getAttributeComparator() {
        return attributeOrdering; // already unmodifiable
    }

    /**
     * Get standard ordering for attribute values.
     * 
     * @return ordered collection with items.
     */
    public static Collection<String> getValueOrder() {
        return valueOrdering.getOrder(); // already unmodifiable
    }

    // note: run FindDTDOrder to get this list
    // TODO, convert to use SupplementalInfo

    private static MapComparator<String> attributeOrdering = new MapComparator<String>()
        .add(
            // START MECHANICALLY attributeOrdering GENERATED BY FindDTDOrder
            "_q type id choice key registry source target path day date version count lines characters before from to iso4217 mzone number time casing list uri digits rounding iso3166 hex request direction alternate backwards caseFirst caseLevel hiraganaQuarternary hiraganaQuaternary variableTop normalization numeric strength elements element attributes attribute attributeValue contains multizone order other replacement scripts services territories territory aliases tzidVersion value values variant variants visibility alpha3 code end exclude fips10 gdp internet literacyPercent locales population writingPercent populationPercent officialStatus start used otherVersion typeVersion access after allowsParsing at bcp47 decexp desired indexSource numberSystem numbers oneway ordering percent priority radix rules supported tender territoryId yeartype cldrVersion grouping inLanguage inScript inTerritory match parent private reason reorder status cashRounding allowed preferred regions validSubLocales standard references alt draft" // END
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // MECHANICALLY
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // attributeOrdering
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // GENERATED
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // BY
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // FindDTDOrder
            .trim().split("\\s+"))
        .setErrorOnMissing(false)
        .freeze();

    private static MapComparator<String> elementOrdering = new MapComparator<String>()
        .add(
            // START MECHANICALLY elementOrdering GENERATED BY FindDTDOrder
            "ldml alternate attributeOrder attributes blockingItems calendarPreference calendarSystem casingData casingItem character character-fallback characterOrder codesByTerritory comment context coverageVariable coverageLevel cp dayPeriodRule dayPeriodRules deprecatedItems distinguishingItems elementOrder first_variable fractions hours identity indexSeparator compressedIndexSeparator indexRangePattern indexLabelBefore indexLabelAfter indexLabel info keyMap languageAlias languageCodes languageCoverage languageMatch languageMatches languagePopulation last_variable first_tertiary_ignorable last_tertiary_ignorable first_secondary_ignorable last_secondary_ignorable first_primary_ignorable last_primary_ignorable first_non_ignorable last_non_ignorable first_trailing last_trailing likelySubtag lineOrder mapKeys mapTypes mapZone numberingSystem parentLocale personList pluralRule pluralRules postCodeRegex primaryZone reference region scriptAlias scriptCoverage serialElements stopwordList substitute suppress tRule telephoneCountryCode territoryAlias territoryCodes territoryCoverage currencyCoverage timezone timezoneCoverage transform typeMap usesMetazone validity alias appendItem base beforeCurrency afterCurrency codePattern contextTransform contextTransformUsage currencyMatch cyclicName cyclicNameContext cyclicNameSet cyclicNameWidth dateFormatItem day dayPeriod dayPeriodContext dayPeriodWidth defaultNumberingSystem deprecated distinguishing blocking coverageAdditions era eraNames eraAbbr eraNarrow exemplarCharacters ellipsis fallback field generic greatestDifference height hourFormat hoursFormat gmtFormat gmtZeroFormat intervalFormatFallback intervalFormatItem key listPattern listPatternPart localeDisplayNames layout contextTransforms localeDisplayPattern languages localePattern localeSeparator localeKeyTypePattern localizedPatternChars dateRangePattern calendars long measurementSystem measurementSystemName messages minDays firstDay month monthPattern monthPatternContext monthPatternWidth months monthNames monthAbbr monthPatterns days dayNames dayAbbr moreInformation native orientation inList inText otherNumberingSystems paperSize pattern displayName quarter quarters quotationStart quotationEnd alternateQuotationStart alternateQuotationEnd rbnfrule regionFormat fallbackFormat fallbackRegionFormat abbreviationFallback preferenceOrdering relative reset import p pc rule ruleset rulesetGrouping s sc scripts segmentation settings short commonlyUsed exemplarCity singleCountries default calendar collation currency currencyFormat currencySpacing currencyFormatLength dateFormat dateFormatLength dateTimeFormat dateTimeFormatLength availableFormats appendItems dayContext dayWidth decimalFormat decimalFormatLength intervalFormats monthContext monthWidth percentFormat percentFormatLength quarterContext quarterWidth scientificFormat scientificFormatLength skipDefaultLocale defaultContent standard daylight stopwords indexLabels mapping suppress_contractions optimize rules surroundingMatch insertBetween symbol decimal group list percentSign nativeZeroDigit patternDigit plusSign minusSign exponential perMille infinity nan currencyDecimal currencyGroup symbols decimalFormats scientificFormats percentFormats currencyFormats currencies t tc q qc i ic extend territories timeFormat timeFormatLength traditional finance transformName type unit unitPattern variable attributeValues variables segmentRules variantAlias variants keys types transformNames measurementSystemNames codePatterns version generation cldrVersion currencyData language script territory territoryContainment languageData territoryInfo postalCodeData calendarData calendarPreferenceData variant week am pm dayPeriods eras cyclicNameSets dateFormats timeFormats dateTimeFormats fields timeZoneNames weekData timeData measurementData timezoneData characters delimiters measurement dates numbers transforms units listPatterns collations posix segmentations rbnf metadata codeMappings parentLocales likelySubtags metazoneInfo mapTimezones plurals telephoneCodeData numberingSystems bcp47KeywordMappings gender references languageMatching dayPeriodRuleSet metaZones primaryZones weekendStart weekendEnd width windowsZones coverageLevels x yesstr nostr yesexpr noexpr zone metazone special zoneAlias zoneFormatting zoneItem supplementalData" // END
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     // MECHANICALLY
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     // elementOrdering
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     // GENERATED
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     // BY
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     // FindDTDOrder
            .trim().split("\\s+"))
        .setErrorOnMissing(false)
        .freeze();

    private static MapComparator<String> valueOrdering = new MapComparator<String>().setErrorOnMissing(false).freeze();
    /*
     * 
     * //RuleBasedCollator valueOrdering = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
     * static {
     * 
     * 
     * // others are alphabetical
     * String[] valueOrder = {
     * "full", "long", "medium", "short",
     * "abbreviated", "narrow", "wide",
     * //"collation", "calendar", "currency",
     * "buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese", "direct",
     * //"japanese", "buddhist", "islamic", "islamic-civil", "hebrew", "chinese", "gregorian", "phonebook",
     * "traditional", "direct",
     * 
     * "sun", "mon", "tue", "wed", "thu", "fri", "sat", // removed, since it is a language tag
     * "America/Vancouver",
     * "America/Los_Angeles",
     * "America/Edmonton",
     * "America/Denver",
     * "America/Phoenix",
     * "America/Winnipeg",
     * "America/Chicago",
     * "America/Montreal",
     * "America/New_York",
     * "America/Indianapolis",
     * "Pacific/Honolulu",
     * "America/Anchorage",
     * "America/Halifax",
     * "America/St_Johns",
     * "Europe/Paris",
     * "Europe/Belfast",
     * "Europe/Dublin",
     * "Etc/GMT",
     * "Africa/Casablanca",
     * "Asia/Jerusalem",
     * "Asia/Tokyo",
     * "Europe/Bucharest",
     * "Asia/Shanghai",
     * };
     * valueOrdering.add(valueOrder).lock();
     * //StandardCodes sc = StandardCodes.make();
     * }
     */
    static MapComparator<String> dayValueOrder = new MapComparator<String>().add(
        "sun", "mon", "tue", "wed", "thu", "fri", "sat").freeze();
    static MapComparator<String> widthOrder = new MapComparator<String>().add(
        "abbreviated", "narrow", "short", "wide", "all").freeze();
    static MapComparator<String> lengthOrder = new MapComparator<String>().add(
        "full", "long", "medium", "short").freeze();
    static MapComparator<String> dateFieldOrder = new MapComparator<String>().add(
        "era", "year", "month", "week", "day", "weekday", "dayperiod",
        "hour", "minute", "second", "zone").freeze();
    static MapComparator<String> countValueOrder = new MapComparator<String>().add(
        "0", "1", "zero", "one", "two", "few", "many", "other").freeze();
    static Comparator<String> zoneOrder = StandardCodes.make().getTZIDComparator();

    static Set<String> orderedElements = Collections.unmodifiableSet(new HashSet<String>(Arrays
        .asList(
            // can prettyprint with TestAttributes

            // DTD: ldml
            // <collation> children
            "base", "optimize", "rules", "settings", "suppress_contractions",

            // <rules> children
            "i", "ic", "p", "pc", "reset", "s", "sc", "t", "tc", "x",

            // <x> children
            "context", "extend", "i", "ic", "p", "pc", "s", "sc", "t", "tc",
            "last_non_ignorable", "last_secondary_ignorable", "last_tertiary_ignorable",

            // <variables> children
            "variable",

            // <rulesetGrouping> children
            "ruleset",

            // <ruleset> children
            "rbnfrule",

            // DTD: supplementalData
            // <territory> children
            // "languagePopulation",

            // <postalCodeData> children
            // "postCodeRegex",

            // <characters> children
            // "character-fallback",

            // <character-fallback> children
            // "character",

            // <character> children
            "substitute", // may occur multiple times

            // <transform> children
            "comment", "tRule",

            // <validity> children
            // both of these don't need to be ordered, but must delay changes until after isDistinguished always uses
            // the dtd type
            "attributeValues", // attribute values shouldn't need ordering, as long as these are distinguishing:
                               // elements="zoneItem" attributes="type"
            "variable", // doesn't need to be ordered

            // <pluralRules> children
            "pluralRule",

            // <codesByTerritory> children
            // "telephoneCountryCode", // doesn't need ordering, as long as code is distinguishing, telephoneCountryCode
            // code="376"

            // <numberingSystems> children
            // "numberingSystem", // doesn't need ordering, since id is distinguishing

            // <metazoneInfo> children
            // "timezone", // doesn't need ordering, since type is distinguishing

            "attributes", // shouldn't need this in //supplementalData/metadata/suppress/attributes, except that the
                          // element is badly designed

            "languageMatch"

        )));

    public static boolean isOrdered(String element, DtdType type) {
        return orderedElements.contains(element);
    }

    /**
   * 
   */
    public static Comparator<String> getAttributeValueComparator(String element, String attribute) {
        Comparator<String> comp = valueOrdering;
        if (attribute.equals("day")) { // && (element.startsWith("weekend")
            comp = dayValueOrder;
        } else if (attribute.equals("type")) {
            if (element.endsWith("FormatLength"))
                comp = lengthOrder;
            else if (element.endsWith("Width"))
                comp = widthOrder;
            else if (element.equals("day"))
                comp = dayValueOrder;
            else if (element.equals("field"))
                comp = dateFieldOrder;
            else if (element.equals("zone")) comp = zoneOrder;
        } else if (attribute.equals("count") && !element.equals("minDays")) {
            comp = countValueOrder;
        }
        return comp;
    }

    /**
     * Comparator for attributes in CLDR files
     */
    public static Comparator<String> ldmlComparator = new LDMLComparator();

    static class LDMLComparator implements Comparator<String> {

        transient XPathParts a = new XPathParts(attributeOrdering, null);
        transient XPathParts b = new XPathParts(attributeOrdering, null);

        public void addElement(String a) {
            // elementOrdering.add(a);
        }

        public void addAttribute(String a) {
            // attributeOrdering.add(a);
        }

        public void addValue(String a) {
            // valueOrdering.add(a);
        }

        public int compare(String o1, String o2) {
            if (o1 == o2) return 0; // quick test for common case
            int result;
            a.set(o1);
            b.set(o2);
            int minSize = a.size();
            if (b.size() < minSize) minSize = b.size();
            for (int i = 0; i < minSize; ++i) {
                String aname = a.getElement(i);
                String bname = b.getElement(i);
                if (0 != (result = elementOrdering.compare(aname, bname))) {
                    // if they are different, then
                    // all ordered items are equal, and > than all unordered
                    boolean aOrdered = orderedElements.contains(aname);
                    boolean bOrdered = orderedElements.contains(bname);
                    // if both ordered, continue, return result
                    if (aOrdered && bOrdered) {
                        // continue with comparison
                    } else {
                        if (aOrdered == bOrdered) return result; // both off
                        return aOrdered ? 1 : -1;
                    }
                }
                Map<String, String> am = a.getAttributes(i);
                Map<String, String> bm = b.getAttributes(i);
                int minMapSize = am.size();
                if (bm.size() < minMapSize) minMapSize = bm.size();
                if (minMapSize != 0) {
                    Iterator ait = am.keySet().iterator();
                    Iterator bit = bm.keySet().iterator();
                    for (int j = 0; j < minMapSize; ++j) {
                        String akey = (String) ait.next();
                        String bkey = (String) bit.next();
                        if (0 != (result = attributeOrdering.compare(akey, bkey))) return result;
                        String avalue = (String) am.get(akey);
                        String bvalue = (String) bm.get(bkey);
                        if (!avalue.equals(bvalue)) {
                            Comparator<String> comp = getAttributeValueComparator(aname, akey);
                            if (0 != (result = comp.compare(avalue, bvalue))) {
                                return result;
                            }
                        }
                    }
                }
                if (am.size() < bm.size()) return -1;
                if (am.size() > bm.size()) return 1;
            }
            if (a.size() < b.size()) return -1;
            if (a.size() > b.size()) return 1;
            return 0;
        }
    }

    private final static Map defaultSuppressionMap;
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
            { "currencyFormat", "type", "standard" },
            { "pattern", "type", "standard" },
            { "currency", "type", "standard" },
            // {"collation", "type", "standard"},
            { "transform", "visibility", "external" },
            { "*", "_q", "*" },
        };
        Map tempmain = asMap(data, true);
        defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
    }

    public static Map getDefaultSuppressionMap() {
        return defaultSuppressionMap;
    }

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
                if (newTemp == null) temp.put(data[i][j], newTemp = tree ? (Map) new TreeMap() : new HashMap());
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
        XPathParts parts = new XPathParts(null, null);
        for (Iterator<String> it = dataSource.iterator(); it.hasNext();) {
            String path = (String) it.next();
            // Value v = (Value) getXpath_value().get(path);
            // if (!(v instanceof StringValue)) continue;
            parts.set(dataSource.getFullPath(path)).addAttribute("draft", draftStatus.toString());
            dataSource.putValueAtPath(parts.toString(), dataSource.getValueAtPath(path));
        }
        return this;
    }

    public UnicodeSet getExemplarSet(String type, WinningChoice winningChoice) {
        return getExemplarSet(type, winningChoice, UnicodeSet.CASE);
    }

    static final UnicodeSet HACK_CASE_CLOSURE_SET = new UnicodeSet(
        "[ſẛﬀẞ{i̇}\u1F71\u1F73\u1F75\u1F77\u1F79\u1F7B\u1F7D\u1FBB\u1FBE\u1FC9\u1FCB\u1FD3\u1FDB\u1FE3\u1FEB\u1FF9\u1FFB\u2126\u212A\u212B]")
        .freeze();

    public UnicodeSet getExemplarSet(String type, WinningChoice winningChoice, int option) {
        if (type.length() != 0) type = "[@type=\"" + type + "\"]";
        String path = "//ldml/characters/exemplarCharacters" + type;
        if (winningChoice == WinningChoice.WINNING) {
            path = getWinningPath(path);
        }
        String v = getStringValue(path);
        if (v == null) return null;
        UnicodeSet result = new UnicodeSet(v);
        UnicodeSet toNuke = new UnicodeSet(HACK_CASE_CLOSURE_SET).removeAll(result);
        result.closeOver(UnicodeSet.CASE);
        result.removeAll(toNuke);
        result.remove(0x20);
        return result;
    }

    public String getCurrentMetazone(String zone) {
        for (Iterator<String> it2 = iterator(); it2.hasNext();) {
            String xpath = (String) it2.next();
            if (xpath.startsWith("//ldml/dates/timeZoneNames/zone[@type=\"" + zone + "\"]/usesMetazone")) {
                XPathParts parts = new XPathParts(null, null);
                parts.set(xpath);
                if (!parts.containsAttribute("to")) {
                    String mz = parts.getAttributeValue(4, "mzone");
                    return mz;
                }
            }
        }
        return null;
    }

    public boolean isResolved() {
        return dataSource.isResolving();
    }

    // WARNING: this must go AFTER attributeOrdering is set; otherwise it uses a null comparator!!
    private static final DistinguishedXPath distinguishedXPath = new DistinguishedXPath();

    // private static Set atomicElements = Collections.unmodifiableSet(new HashSet(Arrays.asList(new
    // String[]{"collation", "segmentation"})));

    public static final String distinguishedXPathStats() {
        return DistinguishedXPath.stats();
    }

    private static class DistinguishedXPath {
        public static final String stats() {
            return "distinguishingMap:" + distinguishingMap.size() + " " +
                "normalizedPathMap:" + normalizedPathMap.size();
        }

        private static Map<String, String> distinguishingMap = new HashMap<String, String>();
        private static Map<String, String> normalizedPathMap = new HashMap<String, String>();
        private static XPathParts distinguishingParts = new XPathParts(attributeOrdering, null);

        public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
            synchronized (distinguishingMap) {
                String result = (String) distinguishingMap.get(xpath);
                if (result == null) {
                    distinguishingParts.set(xpath);

                    // first clean up draft and alt

                    String draft = null;
                    String alt = null;
                    String references = "";
                    // note: we only need to clean up items that are NOT on the last element,
                    // so we go up to size() - 1.

                    // note: each successive item overrides the previous one. That's intended

                    for (int i = 0; i < distinguishingParts.size() - 1; ++i) {
                        // String element = distinguishingParts.getElement(i);
                        // if (atomicElements.contains(element)) break;
                        Map<String, String> attributes = distinguishingParts.getAttributes(i);
                        for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                            String attribute = (String) it.next();
                            if (attribute.equals("draft")) {
                                draft = (String) attributes.get(attribute);
                                it.remove();
                            } else if (attribute.equals("alt")) {
                                alt = (String) attributes.get(attribute);
                                it.remove();
                            } else if (attribute.equals("references")) {
                                if (references.length() != 0) references += " ";
                                references += (String) attributes.get("references");
                                it.remove();
                            }
                        }
                    }
                    if (draft != null || alt != null || references.length() != 0) {
                        // get the last element that is not ordered.
                        int placementIndex = distinguishingParts.size() - 1;
                        while (true) {
                            String element = distinguishingParts.getElement(placementIndex);
                            if (!orderedElements.contains(element)) break;
                            --placementIndex;
                        }
                        Map<String, String> attributes = distinguishingParts.getAttributes(placementIndex);
                        if (draft != null) {
                            attributes.put("draft", draft);
                        }
                        if (alt != null) {
                            attributes.put("alt", alt);
                        }
                        if (references.length() != 0) {
                            attributes.put("references", references);
                        }
                        String newXPath = distinguishingParts.toString();
                        if (!newXPath.equals(xpath)) {
                            normalizedPathMap.put(xpath, newXPath); // store differences
                        }
                    }

                    // now remove non-distinguishing attributes (if non-inheriting)

                    for (int i = 0; i < distinguishingParts.size(); ++i) {
                        String element = distinguishingParts.getElement(i);
                        Map<String, String> attributes = distinguishingParts.getAttributes(i);
                        for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                            String attribute = it.next();
                            if (!isDistinguishing(element, attribute)) {
                                it.remove();
                            }
                        }
                    }

                    result = distinguishingParts.toString();
                    if (result.equals(xpath)) { // don't save the copy if we don't have to.
                        result = xpath;
                    }
                    distinguishingMap.put(xpath, result);
                }
                if (normalizedPath != null) {
                    normalizedPath[0] = (String) normalizedPathMap.get(xpath);
                    if (normalizedPath[0] == null) normalizedPath[0] = xpath;
                }
                return result;
            }
        }

        public Map<String, String> getNonDistinguishingAttributes(String fullPath, Map<String, String> result,
            Set<String> skipList) {
            if (result == null) {
                result = new LinkedHashMap<String, String>();
            } else {
                result.clear();
            }
            synchronized (distinguishingMap) {
                distinguishingParts.set(fullPath);
                for (int i = 0; i < distinguishingParts.size(); ++i) {
                    String element = distinguishingParts.getElement(i);
                    // if (atomicElements.contains(element)) break;
                    Map<String, String> attributes = distinguishingParts.getAttributes(i);
                    for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                        String attribute = it.next();
                        if (!isDistinguishing(element, attribute) && !skipList.contains(attribute)) {
                            result.put(attribute, attributes.get(attribute));
                        }
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
        return dataSource.getDtdVersionInfo();
    }

    public String getStringValue(String path, boolean ignoreOtherLeafAttributes) {
        String result = getStringValue(path);
        if (result != null) return result;
        XPathParts parts = new XPathParts().set(path);
        Map<String, String> lastAttributes = parts.getAttributes(parts.size() - 1);
        XPathParts other = new XPathParts();
        String base = parts.toString(parts.size() - 1) + "/" + parts.getElement(parts.size() - 1); // trim final element
        for (Iterator<String> it = iterator(base); it.hasNext();) {
            String otherPath = it.next();
            other.set(otherPath);
            if (other.size() != parts.size()) {
                continue;
            }
            Map<String, String> lastOtherAttributes = other.getAttributes(other.size() - 1);
            if (!contains(lastOtherAttributes, lastAttributes)) continue;
            if (result == null) {
                result = getStringValue(otherPath);
            } else {
                throw new IllegalArgumentException("Multiple values for path: " + path);
            }
        }
        return result;
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
        XPathParts parts = new XPathParts().set(path);
        Map<String, String> lastAttributes = parts.getAttributes(parts.size() - 1);
        XPathParts other = new XPathParts();
        String base = parts.toString(parts.size() - 1) + "/" + parts.getElement(parts.size() - 1); // trim final element
        for (Iterator<String> it = iterator(base); it.hasNext();) {
            String otherPath = (String) it.next();
            other.set(otherPath);
            if (other.size() != parts.size()) continue;
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
            Relation<String, String> pathMap = new Relation(new HashMap<String, String>(), TreeSet.class,
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
        return getExtraPaths(new HashSet<String>());
    }

    /**
     * Returns the extra paths, skipping those that are already represented in the locale.
     * 
     * @return
     */
    public Collection<String> getExtraPaths(Collection<String> toAddTo) {
        for (String item : getRawExtraPaths()) {
            if (dataSource.getValueAtPath(item) == null) { // don't use getStringValue, since it recurses.
                toAddTo.add(item);
            }
        }
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
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(getSupplementalDirectory());
        Set<String> codes = StandardCodes.make().getAvailableCodes("currency");
        // units
        final Set<Count> pluralCounts = supplementalData.getPlurals(PluralType.cardinal, getLocaleID())
            .getCountToExamplesMap().keySet();
        if (pluralCounts.size() != 1) {
            for (Count count : pluralCounts) {
                for (String unit : new String[] { "year", "month", "week", "day", "hour", "minute", "second" }) {
                    for (String when : new String[] { "", "-past", "-future" }) {
                        toAddTo.add("//ldml/units/unit[@type=\"" + unit + when + "\"]/unitPattern[@count=\""
                            + count + "\"]");
                    }
                    for (String alt : new String[] { "", "[@alt=\"short\"]" }) {
                        toAddTo.add("//ldml/units/unit[@type=\"" + unit + "\"]/unitPattern[@count=\"" + count
                            + "\"]" + alt);
                    }
                }

                for (String unit : codes) {
                    toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + unit + "\"]/displayName[@count=\""
                        + count + "\"]");
                }

                for (String numberSystem : supplementalData.getNumericNumberingSystems()) {
                    String numberSystemString = "[@numberSystem=\"" + numberSystem + "\"]";
                    final String currencyPattern = "//ldml/numbers/currencyFormats" + numberSystemString +
                        "/unitPattern[@count=\"" + count + "\"]";
                    toAddTo.add(currencyPattern);
                    if (DEBUG) {
                        System.out.println(getLocaleID() + "\t" + currencyPattern);
                    }

                    for (String type : new String[] {
                        "1000", "10000", "100000", "1000000", "10000000", "100000000", "1000000000",
                        "10000000000", "100000000000", "1000000000000", "10000000000000", "100000000000000" }) {
                        for (String width : new String[] { "short", "long" }) {
                            toAddTo.add("//ldml/numbers/decimalFormats" +
                                numberSystemString + "/decimalFormatLength[@type=\"" +
                                width + "\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"" +
                                type + "\"][@count=\"" +
                                count + "\"]");
                        }
                    }
                }
            }
        }
        // dayPeriods
        String locale = getLocaleID();
        DayPeriodInfo dayPeriods = supplementalData.getDayPeriods(locale);

        for (String context : new String[] { "format", "stand-alone" }) {
            for (String width : new String[] { "narrow", "abbreviated", "wide" }) {
                if (dayPeriods != null) {
                    LinkedHashSet<DayPeriod> items = new LinkedHashSet<DayPeriod>(dayPeriods.getPeriods());
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
            "Europe/London\"]/long/daylight"
        };
        for (String override : overrides) {
            toAddTo.add("//ldml/dates/timeZoneNames/zone[@type=\"" + override);
        }

        return toAddTo;
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

    private Matcher typeValueMatcher = Pattern.compile("\\[@type=\"([^\"]*)\"\\]").matcher("");

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
                SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(getSupplementalDirectory());
                excludedZones = new HashSet<String>(supplementalData.getSingleRegionZones());
                excludedZones = Collections.unmodifiableSet(excludedZones); // protect
            }
            return excludedZones;
        }
    }

    static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

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
        XPathParts parts = new XPathParts().set(xpath);
        boolean isDisplayName = parts.contains("displayName");

        String intCount = parts.getAttributeValue(-1, "count");
        if (intCount != null && DIGITS.containsAll(intCount)) {
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

    public static List<String> getSerialElements() {
        return new ArrayList<String>(orderedElements);
    }

    public boolean isAliasedAtTopLevel() {
        return iterator("//ldml/alias").hasNext();
    }
}
