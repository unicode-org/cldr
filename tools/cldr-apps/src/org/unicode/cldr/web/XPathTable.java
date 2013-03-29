//
//  XPathTable.java
//  fourjay
//
//  Created by Steven R. Loomis on 20/10/2005.
//  Copyright 2005 IBM. All rights reserved.
//
//

package org.unicode.cldr.web;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.ElapsedTimer;

/**
 * This class maps between full and partial xpaths, and the small integers which
 * are actually stored in the database. It keeps an in-memory cache which is
 * populated as ids are requested.
 */
public class XPathTable {
    public static final String CLDR_XPATHS = "cldr_xpaths";
    private PrettyPath ppath = new PrettyPath();

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
        PreparedStatement queryStmt = conn.prepareStatement("SELECT id,xpath FROM " + CLDR_XPATHS);
        // First, try to query it back from the DB.
        ResultSet rs = queryStmt.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            String xpath = rs.getString(2);
            setById(id, xpath);
            stat_dbFetch++;
            ixpaths++;
        }
        queryStmt.close();
        System.err.println(et + ": " + ixpaths + " loaded");
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
                xpathindex = "xpath(755)";
            }
            sql = ("create table " + CLDR_XPATHS + "(id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", " + "xpath "
                    + DBUtils.DB_SQL_VARCHARXPATH + " not null" + uniqueness + ")");
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
    public Hashtable<String, String> xstringHash = new Hashtable<String, String>(); // public
                                                                                    // for
                                                                                    // statistics
                                                                                    // only
    public Hashtable<String, Integer> stringToId = new Hashtable<String, Integer>(); // public
                                                                                     // for
                                                                                     // statistics
                                                                                     // only
    public Hashtable<Long, String> sidToString = new Hashtable<Long, String>(); // public
                                                                                // for
                                                                                // statistics
                                                                                // only

    public String statistics() {
        return "xstringHash has " + xstringHash.size() + " items.  DB: " + stat_dbAdd + "add/" + stat_dbFetch + "fetch/"
                + (stat_dbAdd + stat_dbFetch) + "total." + "-" + idStats() /*
                                                                            * +
                                                                            * " "
                                                                            * +
                                                                            * strStats
                                                                            * ()
                                                                            */;
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
            addXpaths(unloadedXpaths, conn);
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
        insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_XPATHS + " (xpath) " + " values (" + DBUtils.DB_SQL_BINTRODUCER
                + " ?)");
        for (String xpath : xpaths) {
            insertStmt.setString(1, xpath);
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
            String xpath = rs.getString(2);
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
                    + DBUtils.DB_SQL_BINTRODUCER + " ? " + DBUtils.DB_SQL_BINCOLLATE);
            queryStmt.setString(1, xpath);
            // First, try to query it back from the DB.
            ResultSet rs = queryStmt.executeQuery();
            if (!rs.next()) {
                if (!addIfNotFound) {
                    return -1;
                } else {
                    // add it
                    insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_XPATHS + " (xpath ) " + " values ("
                            + DBUtils.DB_SQL_BINTRODUCER + " ?)", Statement.RETURN_GENERATED_KEYS);

                    insertStmt.setString(1, xpath);
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
            sm.busted("XPathTable: Failed in addXPath(" + xpath + "): " + DBUtils.unchainSqlException(sqe));
        } finally {
            if (inConn != null) {
                conn = null; // don't close
            }
            DBUtils.close(insertStmt, queryStmt, conn);
        }
        return null; // an exception occured.
    }

    /**
     * needs a new name.. This uses the string pool and also adds it to the
     * table
     */
    final String poolx(String x) {
        if (x == null) {
            return null;
        }

        String y = (String) xstringHash.get(x);
        if (y == null) {
            xstringHash.put(x, x);

            // addXpath(x);

            return x;
        } else {
            return y;
        }
    }

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

    public String pathToTinyXpath(String path) {
        return pathToTinyXpath(path, new XPathParts(null, null));
    }

    public String pathToTinyXpath(String path, XPathParts xpp) {
        typeFromPathToTinyXpath(path, xpp);
        return xpp.toString();
    }

    public String typeFromPathToTinyXpath(String path, XPathParts xpp) {
        return whatFromPathToTinyXpath(path, xpp, LDMLConstants.TYPE);
    }

    public String altFromPathToTinyXpath(String path, XPathParts xpp) {
        return whatFromPathToTinyXpath(path, xpp, LDMLConstants.ALT);
    }

    public static String removeAlt(String path) {
        return removeAlt(path, new XPathParts(null, null));
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
        XPathParts xpp = new XPathParts(null, null);
        xpp.initialize(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);

        // Remove alt proposed, but leave the type
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt != null) {
            String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
            if (newAlt == null) {
                lastAtts.remove(LDMLConstants.ALT); // alt dropped out existence
            } else {
                lastAtts.put(LDMLConstants.ALT, newAlt);
            }
        }

        // always remove draft
        lastAtts.remove(LDMLConstants.DRAFT);

        String removed = xpp.toString();

        if (false) {
            String dPath = CLDRFile.getDistinguishingXPath(path, null, false);

            if (!removed.equals(dPath)) {
                System.err.println("RDAP: " + dPath + " vs " + removed);
            }
        }

        return removed;
    }

    private Set<String> undistinguishingAttributes = null;

    public synchronized Set<String> getUndistinguishingElements() {
        if (undistinguishingAttributes == null) {
            Set<String> s = new HashSet<String>(sm.getSupplementalDataInfo().getElementOrder()); // all
                                                                                                 // elements.
                                                                                                 // We
                                                                                                 // assume.
            Collection<String> distinguishing = sm.getSupplementalDataInfo().getDistinguishingAttributes();
            if (distinguishing != null) {
                s.removeAll(distinguishing);
            } else {
                throw new InternalError("Error: 0 attributes are distinguishing!\n");
            }
            s.remove("alt"); // ignore
            s.remove("draft"); // ignore
            undistinguishingAttributes = Collections.unmodifiableSet(s);
        }
        return undistinguishingAttributes;
    }

    public Map<String, String> getUndistinguishingElementsFor(String path, XPathParts xpp) {
        if (path == null) {
            return null;
        }
        Set<String> ue = getUndistinguishingElements();
        xpp.clear();
        xpp.initialize(path);
        Map<String, String> ueMap = null; // common case, none found.
        for (int i = 0; i < xpp.size(); i++) {
            for (String k : xpp.getAttributeKeys(i)) {
                if (!ue.contains(k))
                    continue;
                if (ueMap == null) {
                    ueMap = new TreeMap<String, String>();
                }
                ueMap.put(k, xpp.getAttributeValue(i, k));
            }
        }
        return ueMap;
    }

    public static String removeAlt(String path, XPathParts xpp) {
        xpp.clear();
        xpp.initialize(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        lastAtts.remove(LDMLConstants.ALT);
        return xpp.toString();
    }

    public static String removeDraft(String path) {
        return removeDraft(path, new XPathParts(null, null));
    }

    public static String removeDraft(String path, XPathParts xpp) {
        xpp.clear();
        xpp.initialize(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        lastAtts.remove(LDMLConstants.DRAFT);
        return xpp.toString();
    }

    public static String getAlt(String path) {
        return getAlt(path, new XPathParts(null, null));
    }

    public static String getAlt(String path, XPathParts xpp) {
        xpp.clear();
        xpp.initialize(path);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        return lastAtts.get(LDMLConstants.ALT);
    }

    public static String removeAltFromStub(String stub) {
        int ix = stub.indexOf("[@alt=\"");
        if (ix == -1) {
            return stub;
        }
        int nx = stub.indexOf(']', ix + 1);
        if (nx == -1) {
            return stub; // ?
        }
        return stub.substring(0, ix) + stub.substring(nx + 1);
    }

    public static String removeAttribute(String path, String att) {
        return removeAttribute(path, new XPathParts(null, null), att);
    }

    public static String removeAttribute(String path, XPathParts xpp, String att) {
        xpp.clear();
        xpp.initialize(path);
        Map lastAtts = xpp.getAttributes(-1);
        lastAtts.remove(att);
        return xpp.toString();
    }

    public final int xpathToBaseXpathId(String xpath) {
        return getByXpath(xpathToBaseXpath(xpath));
    }

    public final int xpathToBaseXpathId(int xpath) {
        return getByXpath(xpathToBaseXpath(getById(xpath)));
    }

    /**
     * note does not remove draft. expects a dpath.
     * 
     * @param xpath
     */
    public static String xpathToBaseXpath(String xpath) {
        XPathParts xpp = new XPathParts(null, null);
        xpp.clear();
        xpp.initialize(xpath);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt == null) {
            /*
             * String lelement = xpp.getElement(-1); oldAlt =
             * xpp.findAttributeValue(lelement,LDMLConstants.ALT);
             */
            return xpath; // no change
        }

        String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
        if (newAlt == null) {
            lastAtts.remove(LDMLConstants.ALT); // alt dropped out existence
        } else if (newAlt.equals(oldAlt)) {
            return xpath; // No change
        } else {
            lastAtts.put(LDMLConstants.ALT, newAlt);
        }
        String newXpath = xpp.toString();
        // SurveyLog.logger.warning("xp2Bxp: " + xpath + " --> " + newXpath);
        return newXpath;
    }

    public static String xpathToBaseXpath(String xpath, XPathParts xpp) {
        xpp.initialize(xpath);
        Map<String, String> lastAtts = xpp.getAttributes(-1);
        String oldAlt = (String) lastAtts.get(LDMLConstants.ALT);
        if (oldAlt == null) {
            /*
             * String lelement = xpp.getElement(-1); oldAlt =
             * xpp.findAttributeValue(lelement,LDMLConstants.ALT);
             */
            return xpath; // no change
        }

        String newAlt = LDMLUtilities.parseAlt(oldAlt)[0]; // #0 : altType
        if (newAlt == null) {
            lastAtts.remove(LDMLConstants.ALT); // alt dropped out existence
        } else if (newAlt.equals(oldAlt)) {
            return xpath; // No change
        } else {
            lastAtts.put(LDMLConstants.ALT, newAlt);
        }
        String newXpath = xpp.toString();
        // SurveyLog.logger.warning("xp2Bxp: " + xpath + " --> " + newXpath);
        return newXpath;
    }

    public String whatFromPathToTinyXpath(String path, XPathParts xpp, String what) {
        xpp.clear();
        xpp.initialize(path);
        Map lastAtts = xpp.getAttributes(-1);
        String type = (String) lastAtts.remove(what);
        lastAtts.remove(LDMLConstants.ALT);
        lastAtts.remove(LDMLConstants.TYPE);
        lastAtts.remove(LDMLConstants.DRAFT);
        lastAtts.remove(LDMLConstants.REFERENCES);
        // SurveyLog.logger.warning("Type on " + path + " with -1 is " + type );
        if ((type == null) && (path.indexOf(what) >= 0))
            try {
                // less common case - type isn't the last
                for (int n = -2; (type == null) && ((0 - xpp.size()) < n); n--) {
                    // SurveyLog.logger.warning("Type on n="+n
                    // +", "+path+" with "+n+" is " + type );
                    lastAtts = xpp.getAttributes(n);
                    if (lastAtts != null) {
                        type = (String) lastAtts.remove(what);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // means we ran out of elements.
            }
        return type;
    }

    // proposed-u4-1
    public static final String PROPOSED_U = LDMLConstants.PROPOSED + "-u";
    public static final String PROPOSED_SEP = "-";
    public static final int NO_XPATH = -1;

    public static final String altProposedPrefix(int userid) {
        return PROPOSED_U + userid + PROPOSED_SEP;
    }

    /**
     * parse an alt-proposed, such as "proposed-u4-1" into a userid (4, in this
     * case). returns -1 if altProposed is null or in any way malformed.
     */
    public static final int altProposedToUserid(String altProposed) {
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
     */
    public String getPrettyPath(String path) {
        if (path == null) {
            return null;
        }
        synchronized (ppath) {
            return ppath.getPrettyPath(path);
        }
    }

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
     */
    public String getOriginal(String prettyPath) {
        synchronized (ppath) {
            return ppath.getOriginal(prettyPath);
        }
    }

    public String getOutputForm(String prettyPath) {
        synchronized (ppath) {
            return ppath.getOutputForm(prettyPath);
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

    public long getStringID(int baseXpath) {
        return getStringID(getById(baseXpath));
    }

    public static final long getStringID(String byId) {
        return StringId.getId(byId);
    }

    public String getStringIDString(int baseXpath) {
        return getStringIDString(getById(baseXpath));
    }

    public static final String getStringIDString(String byId) {
        return Long.toHexString(getStringID(byId));
    }
    
    /**
     * Turn a strid into an xpath id
     * @param sid
     * @return
     */
    public final int getXpathIdFromStringId(String sid) {
        return getByXpath(getByStringID(sid));
    }
    

    public String getByStringID(String id) {
        if(id==null) return null;
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

    public void writeXpathFragment(PrintWriter out, boolean xpathSet[]) {
        Map<String, String> m = new TreeMap<String, String>();
        for (int path = 0; path < xpathSet.length; path++) {
            if (xpathSet[path]) {
                m.put(SurveyMain.xmlescape(getById(path)), getStringIDString(path));
            }
        }
        for (Map.Entry<String, String> e : m.entrySet()) {
            out.println("<xpath id=\"" + e.getValue() + "\">" + e.getKey() + "</xpath>");
        }
    }

    public void writeXpathFragment(PrintWriter out, Set<Integer> xpathSet) {
        for (int path : xpathSet) {
            out.println("<xpath id=\"" + getStringIDString(path) + "\">" + SurveyMain.xmlescape(getById(path)) + "</xpath>");
        }
    }

    public void writeXpaths(PrintWriter out, String ourDate, boolean xpathSet[]) {
        out.println("<xpathTable type=\"StringID\" date=\"" + ourDate + "\" >"); // TODO:
                                                                                 // 8601
        writeXpathFragment(out, xpathSet);
        out.println("</xpathTable>");
    }

    /** Old version **/
    public void writeXpaths(PrintWriter out, String ourDate, Set<Integer> xpathSet) {
        out.println("<xpathTable host=\"" + SurveyMain.localhost() + "\" date=\"" + ourDate + "\" type=\"StringID\" count=\""
                + xpathSet.size() + "\" >");
        writeXpathFragment(out, xpathSet);
        out.println("</xpathTable>");
    }
}