//
//  XPathTable.java
//  fourjay
//
//  Created by Steven R. Loomis on 20/10/2005.
//  Copyright 2005 IBM. All rights reserved.
//
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class XPathTable {
    private static java.util.logging.Logger logger;
    
    private static final String CLDR_XPATHS = "cldr_xpaths";
    
    /** 
     * Called by SM to create the reg
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @param isNew  true if should CREATE TABLEs
     */
    public static XPathTable createTable(java.util.logging.Logger xlogger, Connection ourConn, boolean isNew) throws SQLException {
        XPathTable reg = new XPathTable(xlogger,ourConn);
        if(isNew) {
            reg.setupDB();
        }
        reg.myinit();
        logger.info("XPathTable DB: Created.");
        return reg;
    }
    
    /**
     * Called by SM to shutdown
     */
    public void shutdownDB() throws SQLException {
        synchronized(conn) {
            conn.close();
            conn = null;
        }
    }

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException
    {
        logger.info("XPathTable DB: initializing...");
        synchronized(conn) {
            Statement s = conn.createStatement();
            s.execute("create table " + CLDR_XPATHS + "(id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "xpath varchar(150) not null, " +
                                                    "unique(xpath))");
            s.execute("CREATE UNIQUE INDEX unique_xpath on " + CLDR_XPATHS +"(xpath)");
            s.close();
            conn.commit();
        }
    }
    
    Connection conn = null;
    SurveyMain sm = null;
    public Hashtable xstringHash = new Hashtable();  // public for statistics only
    public Hashtable idToString = new Hashtable();  // public for statistics only
    public Hashtable stringToId = new Hashtable();  // public for statistics only

    java.sql.PreparedStatement insertStmt = null;
    java.sql.PreparedStatement queryStmt = null;
    java.sql.PreparedStatement queryIdStmt = null;
    
    public String statistics() {
        return "xstringHash has " + xstringHash.size() + " items.  DB: " + stat_dbAdd +"add/" + stat_dbFetch +
                "fetch/" + stat_allAdds +"total.";
    }
    
    private static int stat_dbAdd = 0;
    private static int stat_dbFetch = 0;
    private static int stat_allAdds = 0;

    public XPathTable(java.util.logging.Logger xlogger, Connection ourConn) {
        logger = xlogger;
        conn = ourConn;
    }
    
    public void myinit() throws SQLException {
        synchronized(conn) {
            insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_XPATHS +" (xpath ) " + 
                                            " values (?)");
            queryStmt = conn.prepareStatement("SELECT id FROM " + CLDR_XPATHS + "   " + 
                                        " where XPATH=?");
            queryIdStmt = conn.prepareStatement("SELECT XPATH FROM " + CLDR_XPATHS + "   " + 
                                        " where ID=?");
        }
    }

    /**
     * Bottleneck 
     */
    private void addXpath(String xpath)
    {
        synchronized(conn) {
            try {
                    queryStmt.setString(1,xpath);
                // First, try to query it back from the DB.
                    ResultSet rs = queryStmt.executeQuery();                
                    if(!rs.next()) {
                        insertStmt.setString(1, xpath);
                        insertStmt.execute();
                        // TODO: Shouldn't there be a way to get the new row's id back??
    //                    logger.info("xpt: added " + xpath);
                        rs = queryStmt.executeQuery();
                        if(!rs.next()) {
                            logger.severe("Couldn't retrieve newly added xpath " + xpath);
                        } else {
                            stat_dbAdd++;
                        }
                    } else {
                        stat_dbFetch++;
                    }
                                    
                    int id = rs.getInt(1);
                    Integer nid = new Integer(id);
                    idToString.put(nid,xpath);
                    stringToId.put(xpath,nid);
    //                logger.info("Mapped " + id + " back to " + xpath);
                    rs.close();
                    stat_allAdds++;
            } catch(SQLException sqe) {
                logger.severe("XPathTable: Failed in addXPath("+xpath+"): " + SurveyMain.unchainSqlException(sqe));
    //            sm.busted("XPathTable: Failed in addXPath: " + SurveyMain.unchainSqlException(sqe));
            }
        }
    }
    
    /** 
     * needs a new name..
     */
    final String poolx(String x) {
        if(x==null) {
            return null;
        }
        String y = (String)xstringHash.get(x);
        if(y==null) {
            xstringHash.put(x,x);
            
            addXpath(x);
            
            return x;
        } else {
            return y;
        }
    }
    
    private String fetchByID(int id) {
        synchronized(conn) {
            try {
                    queryIdStmt.setInt(1,id);
                // First, try to query it back from the DB.
                    ResultSet rs = queryIdStmt.executeQuery();                
                    if(!rs.next()) {
                        rs.close();
                        logger.severe("XPath: no xpath for ID " + id);
                        return null;
                    }
                                    
                    String str = rs.getString(1);
                    Integer nid = new Integer(id);
    /*                idToString.put(nid,xpath);
                    stringToId.put(xpath,nid); */
                    rs.close();
                    return poolx(str); // adds to idtostring and stringtoid
                    // TODO optimize
            } catch(SQLException sqe) {
                logger.severe("XPathTable: Failed ingetByID (ID: "+ id+"): " + SurveyMain.unchainSqlException(sqe) );
    //            sm.busted("XPathTable: Failed in addXPath: " + SurveyMain.unchainSqlException(sqe));
                return null;
            }
        }
    }
    
    /**
     * API for get by ID 
     */
    public final String getById(int id) {
        String s = (String)idToString.get(new Integer(id));
        if(s!=null) {
            return s;
        }
        return fetchByID(id);
    }
}
