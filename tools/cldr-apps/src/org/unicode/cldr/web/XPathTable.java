//
//  XPathTable.java
//  fourjay
//
//  Created by Steven R. Loomis on 20/10/2005.
//  Copyright 2005-2014 IBM. All rights reserved.
//
//

package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.impl.Utility;

/**
 * This class maps between full and partial xpaths, and the small integers (xpids) which
 * are actually stored in the database. It keeps an in-memory cache which is
 * populated as ids are requested.
 *
 *
 * Definitions:
 *    xpath:        an XPath, such as "//ldml/shoeSize"
 *    xpid / int:   an integer 'token' value, such as 123.
 *                   This is old and deprecated.
 *                   Specific to this instance of SurveyTool.
 *                   This is usually what is meant by "int xpath" in code or in the database.
 *    strid / hex:  a hexadecimal "hash" of the full xpath such as "b1dfb436c841a73".
 *                   This is the preferred method of condensing of xpaths.
 *                   Note that you can't calculate the xpath from this without a look-up table.
 *    "long" StringID:    this is the "long" form of the hex id.  Not used within the SurveyTool, some CLDR tools use it.
 */
public class XPathTable {
    public static final String CLDR_XPATHS = "cldr_xpaths";

    private PrettyPath ppath = new PrettyPath();

    private static final boolean DEBUG = false;

    /**
     * Called by SM to create the reg
     *
     * @param ourConn
     *            the conn to use
     */
    public static XPathTable createTable(Connection ourConn, SurveyMain sm) throws SQLException {
        try {
            boolean isNew = !DBUtils.hasTable(ourConn, CLDR_XPATHS);
            XPathTable reg = new XPathTable();
            reg.sm = sm;
            if (isNew) {
                reg.setupDB();
            }

            if (false && !sm.isUnofficial()) { // precache
                int n = 0;
                ElapsedTimer et = new ElapsedTimer("Load all xpaths..");
                for (String xpath : sm.getBaselineFile()) {
                    reg.getByXpath(xpath, ourConn);
                    n++;
                }
                SurveyLog.logger.warning(et + " (" + n + " xpaths from " + sm.BASELINE_ID + ") " + reg.statistics());
            }

            reg.loadXPaths(ourConn);

            return reg;
        } finally {
            DBUtils.closeDBConnection(ourConn);
        }
    }

    private void loadXPaths(Connection conn) throws SQLException {
        if (stringToId.size() != 0) { // Only load the entire stringToId map
            // once.
            return;
        }
        ElapsedTimer et = new ElapsedTimer("XPathTable: load all xpaths");
        int ixpaths = 0;
        PreparedStatement queryStmt = DBUtils.prepareForwardReadOnly(conn, "SELECT id,xpath FROM " + CLDR_XPATHS);
        // First, try to query it back from the DB.
        ResultSet rs = queryStmt.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            String xpath = Utility.unescape(rs.getString(2));
            setById(id, xpath);
            stat_dbFetch++;
            ixpaths++;
        }
        queryStmt.close();
        final boolean hushMessages = CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST;
        if (!hushMessages) System.err.println(et + ": " + ixpaths + " loaded");
    }

    /**
     * Called by SM to shutdown
     *
     * @deprecated unneeded
     */
    public void shutdownDB() throws SQLException {

    }

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException {
        String sql = null;
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            SurveyLog.debug("XPathTable DB: initializing... conn: " + conn + ", db:" + CLDR_XPATHS + ", id:"
                + DBUtils.DB_SQL_IDENTITY);
            Statement s = conn.createStatement();
            if (s == null) {
                throw new InternalError("S is null");
            }
            String xpathindex = "xpath";
            String uniqueness = ", " + "unique(xpath)";
            if (DBUtils.db_Mysql) {
                uniqueness = "";
                xpathindex = "xpath(768)";
            }
            sql = ("create table " + CLDR_XPATHS + "(id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", " + "xpath "
                + DBUtils.DB_SQL_VARCHARXPATH + DBUtils.DB_SQL_MB4 + " NOT NULL" + uniqueness + ") " + DBUtils.DB_SQL_ENGINE_INNO + DBUtils.DB_SQL_MB4);
            s.execute(sql);
            sql = ("CREATE UNIQUE INDEX unique_xpath on " + CLDR_XPATHS + " (" + xpathindex + ")");
            s.execute(sql);
            sql = ("CREATE INDEX " + CLDR_XPATHS + "_id on " + CLDR_XPATHS + "(id)");
            s.execute(sql);
            sql = ("CREATE INDEX " + CLDR_XPATHS + "_xpath on " + CLDR_XPATHS + " (" + xpathindex + ")");
            s.execute(sql);
            sql = null;
            s.close();
            conn.commit();
        } finally {
            DBUtils.close(conn);
            if (sql != null) {
                SurveyLog.logger.warning("Last SQL: " + sql);
            }
        }
    }

    SurveyMain sm = null;
    //    public Hashtable<String, String> xstringHash = new Hashtable<String, String>(4096); // public for statistics only
    public Hashtable<String, Integer> stringToId = new Hashtable<String, Integer>(4096); // public for statistics only
    public Hashtable<Long, String> sidToString = new Hashtable<Long, String>(4096); // public for statistics only

    public String statistics() {
        return /*"xstringHash has " + xstringHash.size() + " items.  "+*/"DB: " + stat_dbAdd + "add/" + stat_dbFetch + "fetch/"
            + (stat_dbAdd + stat_dbFetch) + "total." + "-" + idStats();
    }

    private static int stat_dbAdd = 0;
    private static int stat_dbFetch = 0;

    public XPathTable() {
    }

    /**
     * SpecialTable implementation
     */
    IntHash<String> xptHash = new IntHash<String>();

    String idToString_put(int id, String str) {
        return xptHash.put(id, str);
    }

    String idToString_get(int id) {
        return xptHash.get(id);
    }

    String idStats() {
        return xptHash.stats();
    }

    /** END specialtable implementation */

    /**
     * Loads all xpath-id mappings from the database. If there are any xpaths in
     * the specified XMLSource which are not already in the database, they will
     * be created here.
     */
    public synchronized void loadXPaths(XMLSource source) {
        // Get list of xpaths that aren't already loaded.
        Set<String> unloadedXpaths = new HashSet<String>();
        for (String xpath : source) {
            unloadedXpaths.add(xpath);
        }
        unloadedXpaths.removeAll(stringToId.keySet());

        Connection conn = null;
        PreparedStatement queryStmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            if (!DEBUG) {
                addXpaths(unloadedXpaths, conn);
            } else {
                // Debug: add paths one by one.
                for (final String path : unloadedXpaths) {
                    try {
                        addXpaths(Collections.singleton(path), conn);
                    } catch (SQLException se) {
                        SurveyLog.logException(se, "While loading xpath: " + Utility.escape(path));
                        throw se;
                    }
                }
            }
        } catch (SQLException sqe) {
            SurveyLog.logException(sqe, "loadXPaths(" + source.getLocaleID() + ")");
            SurveyMain.busted("loadXPaths(" + source.getLocaleID() + ")", sqe);
        } finally {
            DBUtils.close(queryStmt, conn);
        }
    }

    /**
     * Add a set of xpaths to the database.
     *
     * @param xpaths
     * @param conn
     * @throws SQLException
     */
    private synchronized void addXpaths(Set<String> xpaths, Connection conn) throws SQLException {
        if (xpaths.size() == 0)
            return;

        PreparedStatement queryStmt = null;
        PreparedStatement insertStmt = null;
        // Insert new xpaths.
        insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_XPATHS + " (xpath) " + " values ("
            + " ?)");
        for (String xpath : xpaths) {
            insertStmt.setString(1, Utility.escape(xpath));
            insertStmt.addBatch();
            stat_dbAdd++;
        }
        insertStmt.executeBatch();
        conn.commit();
        insertStmt.close();

        // PreparedStatement.getGeneratedKeys() only returns the ID of the
        // last INSERT statement, so we have to improvise here by performing
        // another SELECT to get the newly-inserted IDs.
        queryStmt = conn.prepareStatement("SELECT id,xpath FROM " + CLDR_XPATHS + " ORDER BY id DESC");
        queryStmt.setMaxRows(xpaths.size());
        queryStmt.setFetchSize(xpaths.size());
        ResultSet rs = queryStmt.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            String xpath = Utility.unescape(rs.getString(2));
            setById(id, xpath);
        }
        queryStmt.close();
    }

    /**
     * @return the xpath's id (as an Integer)
     */
    private synchronized Integer addXpath(String xpath, boolean addIfNotFound, Connection inConn) {
        Integer nid = (Integer) stringToId.get(xpath); // double check
        if (nid != null) {
            return nid;
        }

        Connection conn = null;
        PreparedStatement queryStmt = null;
        PreparedStatement insertStmt = null;
        try {
            if (inConn != null) {
                conn = inConn;
            } else {
                conn = DBUtils.getInstance().getDBConnection();
            }
            queryStmt = conn.prepareStatement("SELECT id FROM " + CLDR_XPATHS + "   " + " where XPATH="
                + " ? ");
            queryStmt.setString(1, Utility.escape(xpath));
            // First, try to query it back from the DB.
            ResultSet rs = queryStmt.executeQuery();
            if (!rs.next()) {
                if (!addIfNotFound) {
                    return -1;
                } else {
                    // add it
                    insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_XPATHS + " (xpath ) " + " values ("
                        + " ?)", Statement.RETURN_GENERATED_KEYS);

                    insertStmt.setString(1, Utility.escape(xpath));
                    insertStmt.execute();
                    conn.commit();
                    rs = insertStmt.getGeneratedKeys();
                    if (!rs.next()) {
                        SurveyLog.errln("Couldn't retrieve newly added xpath " + xpath);
                    } else {
                        stat_dbAdd++;
                    }
                }
            } else {
                stat_dbFetch++;
            }

            int id = rs.getInt(1);
            nid = Integer.valueOf(id);
            setById(id, xpath);
            // logger.info("Mapped " + id + " back to " + xpath);
            rs.close();
            return nid;
        } catch (SQLException sqe) {
            SurveyLog.logger.warning("xpath [" + xpath + "] len " + xpath.length());
            SurveyLog.logger.severe("XPathTable: Failed in addXPath(" + xpath + "): " + DBUtils.unchainSqlException(sqe));
            SurveyMain.busted("XPathTable: Failed in addXPath(" + xpath + "): " + DBUtils.unchainSqlException(sqe));
        } finally {
            if (inConn != null) {
                conn = null; // don't close
            }
            DBUtils.close(insertStmt, queryStmt, conn);
        }
        return null; // an exception occured.
    }

    //    /**
    //     * needs a new name.. This uses the string pool and also adds it to the
    //     * table
    //     */
    //    final String poolx(String x) {
    //        if (x == null) {
    //            return null;
    //        }
    //
    //        String y = (String) xstringHash.get(x);
    //        if (y == null) {
    //            xstringHash.put(x, x);
    //
    //            // addXpath(x);
    //
    //            return x;
    //        } else {
    //            return y;
    //        }
    //    }

    /**
     * API for get by ID
     */
    public final String getById(int id) {
        if (id == -1) {
            return null;
        }
        String s = idToString_get(id);
        if (s != null) {
            return s;
        }
        throw new RuntimeException(id + " not found. Make sure loadXpaths() was called first!");
    }

    /**
     * Adds an xpathid-xpath value pair to the XPathTable. This method is used
     * by classes to cache the values obtained by using their own queries.
     *
     * @param id
     * @param xpath
     */
    public final void setById(int id, String xpath) {
        stringToId.put(idToString_put(id, xpath), id);
        sidToString.put(getStringID(xpath), xpath);
    }

    /**
     * get an xpath id by value, add it if not found
     *
     * @param xpath
     *            string string to add
     * @return the id for the specified path
     */
    public final int getByXpath(String xpath) {
        Integer nid = (Integer) stringToId.get(xpath);
        if (nid != null) {
            return nid.intValue();
        } else {
            return addXpath(xpath, true, null).intValue();
        }
    }

    /**
     * Look up xpath id by value. Return -1 if not found
     *
     * @param xpath
     * @return id, or -1 if not found
     */
    public final int peekByXpath(String xpath) {
        Integer nid = (Integer) stringToId.get(xpath);
        if (nid != null) {
            return nid.intValue();
        } else {
            return addXpath(xpath, false, null).intValue();
        }
    }

    /**
     * get an xpath id by value, add it if not found
     *
     * @param xpath
     *            string string to add
     * @return the id for the specified path
     */
    public final int getByXpath(String xpath, Connection conn) {
        Integer nid = (Integer) stringToId.get(xpath);
        if (nid != null) {
            return nid.intValue();
        } else {
            return addXpath(xpath, true, conn).intValue();
        }
    }

    /**
     * Look up xpath id by value. Return -1 if not found
     *
     * @param xpath
     * @return id, or -1 if not found
     */
    public final int peekByXpath(String xpath, Connection conn) {
        Integer nid = (Integer) stringToId.get(xpath);
        if (nid != null) {
            return nid.intValue();
        } else {
            return addXpath(xpath, false, conn).intValue();
        }
    }

    /**
     * 
     * @param path
     * @param xpp
     * @return
     */
    public String altFromPathToTinyXpath(String path) {
        return whatFromPathToTinyXpath(path, LDMLConstants.ALT);
    }

    /**
     * 
     * @param path
     * @return
     * 
     * Called by handlePathValue, by makeProposedFile, and by doForum (which is possibly never called?)
     */
    public static String removeAlt(String path) {
        XPathParts xpp = XPathParts.getTestInstance(path);
        xpp.removeAttribute(-1, LDMLConstants.ALT);
        return xpp.toString();
    }

    /**
     * remove the 'draft=' and 'alt=*proposed' from the XPath. Makes the path
     * almost distinguishing, except that certain attributes, such as numbers=,
     * will be left.
     *
     * @param path
     * @return
     */
    public static String removeDraftAltProposed(String path) {
        XPathParts xpp = XPathParts.getTestInstance(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);

        // Remove alt proposed, but leave the type
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt != null) {
            String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
            if (newAlt == null) {
                xpp.removeAttribute(-1, LDMLConstants.ALT); // alt dropped out existence
            } else {
                xpp.putAttributeValue(-1, LDMLConstants.ALT, newAlt);
            }
        }

        // always remove draft
        xpp.removeAttribute(-1, LDMLConstants.DRAFT);

        return xpp.toString();
    }

    public Map<String, String> getUndistinguishingElementsFor(String path, XPathParts xpp) {
        return XPathParts.getFrozenInstance(path).getSpecialNondistinguishingAttributes();
    }

    /**
     * 
     * @param path
     * @return
     * 
     * Called by handlePathValue and makeProposedFile
     */
    public static String getAlt(String path) {
        XPathParts xpp = XPathParts.getTestInstance(path);
        return xpp.getAttributeValue(-1, LDMLConstants.ALT);
    }

    public final int xpathToBaseXpathId(String xpath) {
        return getByXpath(xpathToBaseXpath(xpath));
    }

    /**
     * note does not remove draft. expects a dpath.
     *
     * @param xpath
     * 
     * This is NOT the same as the two-parameter xpathToBaseXpath elsewhere in this file
     */
    public static String xpathToBaseXpath(String xpath) {
        XPathParts xpp = XPathParts.getTestInstance(xpath);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt == null) {
            return xpath; // no change
        }

        String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
        if (newAlt == null) {
            xpp.removeAttribute(-1, LDMLConstants.ALT); // alt dropped out existence
        } else if (newAlt.equals(oldAlt)) {
            return xpath; // No change
        } else {
            xpp.putAttributeValue(-1, LDMLConstants.ALT, newAlt);
        }
        String newXpath = xpp.toString();
        // SurveyLog.logger.warning("xp2Bxp: " + xpath + " --> " + newXpath);
        return newXpath;
    }

    /**
     * Modify the given XPathParts by possibly changing or removing its ALT attribute.
     *
     * @param xpp the XPathParts, whose contents get changed here and used/modified by the caller
     *
     * Called only from submit.jsp
     */
    public static void xPathPartsToBase(XPathParts xpp) {
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt == null) {
            return; // no change
        }
        String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
        if (newAlt == null) {
            xpp.removeAttribute(-1, LDMLConstants.ALT); // alt dropped out existence
        } else if (newAlt.equals(oldAlt)) {
            return; // No change
        } else {
            xpp.putAttributeValue(-1, LDMLConstants.ALT, newAlt);
        }
    }

    /**
     * Get the type attribute for the given path and LDMLConstants
     *
     * @param path
     * @param what LDMLConstants.ALT
     * @return the type as a string
     */
    private String whatFromPathToTinyXpath(String path, String what) {
        XPathParts xpp = XPathParts.getTestInstance(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        String type = lastAtts.get(what);
        if (type != null) {
            xpp.removeAttribute(-1, what);
        }
        xpp.removeAttribute(-1, LDMLConstants.ALT);
        xpp.removeAttribute(-1, LDMLConstants.TYPE);
        xpp.removeAttribute(-1, LDMLConstants.DRAFT);
        xpp.removeAttribute(-1, LDMLConstants.REFERENCES);
        // SurveyLog.logger.warning("Type on " + path + " with -1 is " + type );
        if ((type == null) && (path.indexOf(what) >= 0)) {
            try {
                // less common case - type isn't the last
                for (int n = -2; (type == null) && ((0 - xpp.size()) < n); n--) {
                    // SurveyLog.logger.warning("Type on n="+n
                    // +", "+path+" with "+n+" is " + type );
                    lastAtts = xpp.getAttributes(n);
                    if (lastAtts != null) {
                        type = lastAtts.get(what);
                        if (type != null) {
                            xpp.removeAttribute(n, what);
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // means we ran out of elements.
            }
        }
        return type;
    }

    // proposed-u4-1
    public static final String PROPOSED_U = LDMLConstants.PROPOSED + "-u";
    public static final String PROPOSED_SEP = "-";
    public static final String PROPOSED_V = "v";
    public static final int NO_XPATH = -1;

    public static final StringBuilder appendAltProposedPrefix(StringBuilder sb, int userid, Integer voteValue) {
        sb.append(PROPOSED_U);
        sb.append(userid);
        if (voteValue != null) {
            sb.append(PROPOSED_V);
            sb.append(voteValue);
        }
        sb.append(PROPOSED_SEP);
        return sb;
    }

    /**
     * parse an alt-proposed, such as "proposed-u4-1" into a userid (4, in this
     * case). returns -1 if altProposed is null or in any way malformed.
     */
    public static final int altProposedToUserid(String altProposed, Integer voteValue[]) {
        if ((altProposed == null) || !altProposed.contains(PROPOSED_U)) {
            return -1;
        }
        // skip over 'proposed-u'
        String idStr = altProposed.substring(altProposed.indexOf(PROPOSED_U) + PROPOSED_U.length());
        int dash;
        if (-1 != (dash = idStr.indexOf(PROPOSED_SEP))) {
            idStr = idStr.substring(0, dash);
        }
        try {
            return Integer.parseInt(idStr);
        } catch (Throwable t) {
            return -1;
        }
    }

    // re export PrettyPath API but synchronized
    /**
     * Gets sortable form of the pretty path, and caches the mapping for faster
     * later mapping.
     *
     * @param path
     * @deprecated PrettyPath is deprecated.
     */
    @Deprecated
    public String getPrettyPath(String path) {
        if (path == null) {
            return null;
        }
        synchronized (ppath) {
            return ppath.getPrettyPath(path);
        }
    }

    /**
     * @deprecated PrettyPath
     * @param path
     * @return
     */
    @Deprecated
    public String getPrettyPath(int path) {
        if (path == -1) {
            return null;
        }
        return getPrettyPath(getById(path));
    }

    /**
     * Get original path. ONLY works if getPrettyPath was called with the
     * original!
     *
     * @param prettyPath
     * @return original path
     * @deprecated PrettyPath
     */
    @Deprecated
    public String getOriginal(String prettyPath) {
        synchronized (ppath) {
            return ppath.getOriginal(prettyPath);
        }
    }

    /**
     * How much is inside?
     *
     * @return Number of xpaths in the table
     */
    public int count() {
        return stringToId.size();
    }

    /**
     * xpath to long
     * @param xpath a string identifying a path, for example "//ldml/numbers/symbols[@numberSystem="sund"]/infinity"
     * @return a long integer, which is a hash of xpath; for example 2795888612892500012 (decimal) = 6d37a14eec91cee6 (hex)
     */
    public static final long getStringID(String xpath) {
        return StringId.getId(xpath);
    }

    /**
     * xpid to hex
     * @param baseXpath
     * @return
     */
    public String getStringIDString(int baseXpath) {
        return getStringIDString(getById(baseXpath));
    }

    /**
     * xpath to hex
     * @param xpath a string identifying a path, for example "//ldml/numbers/symbols[@numberSystem="sund"]/infinity"
     * @return a sixteen-digit hex string, which is a hash of xpath; for example "6d37a14eec91cee6"
     */
    public static final String getStringIDString(String xpath) {
        return Long.toHexString(getStringID(xpath));
    }

    /**
     * Turn a strid into a xpid (int token)
     * @param sid
     * @return
     */
    public final int getXpathIdFromStringId(String sid) {
        return getByXpath(getByStringID(sid));
    }

    /**
     * Given an XPath stringid, return an integer xpid or NO_XPATH
     * This function is there to ease transition away from xpids.
     * @param xpath a StringID (hex) or a decimal id of the form "#1234"
     * @return the integer xpid or  {@link XPathTable#NO_XPATH}
     */
    public int getXpathIdOrNoneFromStringID(String xpath) {
        int base_xpath;
        if (xpath == null || xpath.isEmpty()) {
            base_xpath = XPathTable.NO_XPATH;
        } else if (xpath.startsWith("#")) {
            base_xpath = Integer.parseInt(xpath.substring(1));
        } else {
            base_xpath = getXpathIdFromStringId(xpath);
        }
        return base_xpath;
    }

    public String getByStringID(String id) {
        if (id == null) return null;
        Long l = Long.parseLong(id, 16);
        String s = sidToString.get(l);
        if (s != null)
            return s;
        // slow way
        for (String x : stringToId.keySet()) {
            if (getStringID(x) == l) {
                sidToString.put(l, x);
                return x;
            }
        }
        if (SurveyMain.isUnofficial())
            System.err.println("xpt: Couldn't find stringid " + id + " - sid has " + sidToString.size());
        // it may be
        return null;
    }

}