/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

//  Created by Steven R. Loomis on 31/10/2005.
//  an XMLSource which is implemented in a database

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

// JDBC imports
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

// CLDR imports
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.XMLSource.Alias;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.icu.LDMLConstants;

// ICU
import com.ibm.icu.util.ULocale;

public class CLDRDBSource extends XMLSource {
    private static java.util.logging.Logger logger;
 // local things
    private Comments xpath_comments = new Comments(); // map from paths to comments.  (for now: empty. TODO: make this real)
    public CLDRFile.Factory  factory = null; // TODO: 0 make private
    private String dir = null; // LDML dir
 // DB things
    public static final String CLDR_DATA = "cldr_data";
    public static final String CLDR_SRC = "cldr_src";
    
    private String tree = "main";
    
    protected XPathTable xpt = null;         // XPathTable (shared) that is keyed to this database.
    protected UserRegistry.User user= null; // User this File belongs to (or null)
    protected Connection conn = null;
    /** 
     * called once (at DB setup time) to initialize the database.
     */
    
    public static void setupDB(java.util.logging.Logger xlogger, Connection sconn, boolean isNew) throws SQLException
    {
        logger = xlogger; // set static
        if(!isNew) {
            return; // nothing to setup
        }
        logger.info("CLDRDBSource DB: initializing...");
        synchronized(sconn) {
        String sql;
            Statement s = sconn.createStatement();
            sql = "create table " + CLDR_DATA + " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "xpath INT not null, " + // normal
                                                    "txpath INT not null, " + // tiny
                                                    "locale varchar(20), " +
                                                    "source INT, " +
                                                    "origxpath INT not null, " + // full
                                                    "alt_proposed varchar(50), " +
                                                    "alt_type varchar(50), " +
                                                    "type varchar(50), " +
                                                    "value varchar(2048) not null, " +
                                                    "submitter INT, " +
                                                    "modtime TIMESTAMP )";
//            System.out.println(sql);
            s.execute(sql);
            sql = "create table " + CLDR_SRC + " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "locale varchar(20), " +
                                                    "tree varchar(20) NOT NULL, " +
                                                    "rev varchar(20), " +
                                                    "modtime TIMESTAMP )";
//            System.out.println(sql);
            s.execute(sql);
//            s.execute("CREATE UNIQUE INDEX unique_xpath on " + CLDR_DATA +"(xpath)");
            s.close();
            sconn.commit();
        }
        logger.info("CLDRDBSource DB: done.");
    }

    PreparedStatement insertStmt = null;
    PreparedStatement queryStmt = null;
    PreparedStatement queryValueStmt = null;
    PreparedStatement queryXpathTypes = null;
    PreparedStatement queryTypeValues = null;
    PreparedStatement queryXpathPrefixes = null;
    PreparedStatement keySetStmt = null;
    PreparedStatement queryIdStmt = null;
    PreparedStatement querySourceStmt = null;
    PreparedStatement insertSourceStmt = null;
    
    private PreparedStatement prepareStatement(String name, String sql) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
        } catch ( SQLException se ) {
            String complaint = " Couldn't prepare " + name + " - " + SurveyMain.unchainSqlException(se) + " - " + sql;
            logger.severe(complaint);
            throw new InternalError(complaint);
        }
        return ps;
    }
    
    /** 
     * Initialize with (a reference to the shared) Connection
     * set up statements, etc.
     *
     * Verify that the Source data is available.
     */
    private void initConn(Connection newConn, CLDRFile.Factory theFactory) {
        conn = newConn;
        synchronized(conn) {
            insertStmt = prepareStatement("insertStmt",
                        "INSERT INTO " + CLDR_DATA +
                        " (xpath,locale,source,origxpath,value,type,alt_type,txpath) " +
                        "VALUES (?,?,?,?,?,?,?,?)");

            queryXpathPrefixes = prepareStatement("queryXpathPrefixes",
                "select "+XPathTable.CLDR_XPATHS+".id from "+
                        XPathTable.CLDR_XPATHS+","+CLDR_DATA+" where "+CLDR_DATA+".txpath="+
                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ? AND "+CLDR_DATA+".locale=?");

            queryXpathTypes = prepareStatement("queryXpathTypes",
                "select " +CLDR_DATA+".type from "+
                        CLDR_DATA+","+XPathTable.CLDR_XPATHS+" where "+CLDR_DATA+".xpath="+
                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ?");

            queryTypeValues = prepareStatement("queryTypeValues",
                "select "+CLDR_DATA+".value,"+CLDR_DATA+".alt_type,"+CLDR_DATA+".alt_proposed,"+CLDR_DATA+".submitter" +
                        " from "+
                        CLDR_DATA+" where "+CLDR_DATA+".txpath=? AND "+CLDR_DATA+".locale=? AND "+CLDR_DATA+".type=?");

            queryValueStmt = prepareStatement("queryValueStmt",
                "SELECT value FROM " + CLDR_DATA +
                        " WHERE locale=? AND xpath=?"); // TODO: 1 need to be more specific! ! ! !

            keySetStmt = prepareStatement("keySetStmt",
                "SELECT xpath,origxpath FROM " + CLDR_DATA +
                        " WHERE locale=?"); // TODO: 1 need to be more specific!

            querySourceStmt = prepareStatement("querySourceStmt",
                "SELECT id,rev FROM " + CLDR_SRC + " where locale=? AND tree=?");

            insertSourceStmt = prepareStatement("insertSourceStmt",
                "INSERT INTO " + CLDR_SRC + " (locale,tree,rev) VALUES (?,?,?)");
        }
        factory = theFactory;
        
        if(!loadAndValidate(getLocaleID(), null)) {
            throw new InternalError("Couldn't load and validate: " + getLocaleID());
        }
    }
    
    boolean loadAndValidate(String locale, WebContext forUser) {
        if(conn == null) {
            throw new InternalError("loadAndValidate: failed, no DB connection");
        }

        synchronized(conn) {
            CLDRFile file = factory.make(locale, false, true);
            if(file == null) {
                logger.severe("Couldn't load CLDRFile for " + locale);
                return false;
            }
            
            logger.info("Loaded CLDRFile for " + locale);
            
            int srcId = getSourceId(tree, locale);
            if(srcId == -1) {
                    String rev=LDMLUtilities.getCVSVersion(dir, locale+".xml");
                    srcId = setSourceId(tree, locale, rev);
                    logger.info("adding rev: " + rev + " for " + dir + ":" + locale+".xml");
            } else {
                logger.info("Assume already added - - - exitting!");
                return true;
            }
            logger.info("srcid: " + srcId);
            
            // Now, start loading stuff in
            XPathParts xpp=new XPathParts(null,null);
            for (Iterator it = file.iterator(); it.hasNext();) {
                String xpath = (String) it.next();
                String oxpath = file.getFullXPath(xpath);
                int xpid = xpt.getByXpath(xpath);
                int oxpid = xpt.getByXpath(oxpath);
                String value = file.getStringValue(xpath);
                xpp.clear();
                xpp.initialize(oxpath);
                String lelement = xpp.getElement(-1);
                String eType = xpp.findAttributeValue(lelement,LDMLConstants.TYPE);
                String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
                String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);

                // now, cut out the parts we dont want
                xpp.clear();
                xpp.initialize(xpath);
                Map lastAtts = xpp.getAttributes(-1);
                lastAtts.remove(LDMLConstants.TYPE);
                lastAtts.remove(LDMLConstants.ALT);
                lastAtts.remove(LDMLConstants.DRAFT);
                
                String tinyXpath = xpp.toString();
                int txpid = xpt.getByXpath(tinyXpath);
                
                /*SRL*/ 
                System.out.println(xpath + " l: " + locale);
                System.out.println(" <- " + oxpath);
                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
                System.out.println(" => "+txpid+"#" + tinyXpath);
                  
                try {
                    insertStmt.setInt(1,xpid); // full
                    insertStmt.setString(2,locale);
                    insertStmt.setInt(3,srcId);
                    insertStmt.setInt(4,oxpid); // Note: assumes XPIX = orig XPID! TODO: fix
                    insertStmt.setString(5,value);
                    insertStmt.setString(6,eType);
                    insertStmt.setString(7,eAlt);
                    insertStmt.setInt(8,txpid); // tiny

                    insertStmt.execute();
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't insert " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
            }
            
            try{
                    conn.commit();
            } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't commit " + locale + ":" + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
            }
            
            return true;
        }
    }
    
    int getSourceId(String tree, String locale) {
        synchronized (conn) {
            try {
                querySourceStmt.setString(1,locale);
                querySourceStmt.setString(2,tree);
                ResultSet rs = querySourceStmt.executeQuery();
                if(!rs.next()) {
                    rs.close();
                    return -1;
                }
                int result = rs.getInt(1);
                if(rs.next()) {
                    logger.severe("Source returns two results: " + tree + "/" + locale);
                    return -1;
                }
                rs.close();
                return result;
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to find source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return -1;
            }
        }
    }
    
    int setSourceId(String tree, String locale, String rev) {
        synchronized (conn) {
            try {
                insertSourceStmt.setString(1,locale);
                insertSourceStmt.setString(2,tree);
                insertSourceStmt.setString(3,rev);
                if(!insertSourceStmt.execute()) {
                    conn.commit();
                    return getSourceId(tree, locale);
                } else {
                    conn.commit();
                    logger.severe("CLDRDBSource: SQL failed to set source ("+tree + "/" + locale +")");
                    return -1;
                }
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to set source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return -1;
            }
        }
    }
    
	protected void putFullPath(String distinguishingXPath, String fullxpath) {
        // TODO: 0
    }
    
	protected void putValue(String distinguishingXPath, String value) {
        // TODO: 0
    }
    
	public String getValue(String path) {
        if(conn == null) {
            throw new InternalError("No DB connection!");
        }
    
        String locale = getLocaleID();
        synchronized (conn) {
            try {
                queryValueStmt.setString(1,locale);
                queryValueStmt.setInt(2,xpt.getByXpath(path)); // TODO: 2 more specificity
                ResultSet rs = queryValueStmt.executeQuery();
                String rv;
                if(!rs.next()) {
                    rs.close();
                    return null;
                }
                rv = rs.getString(1);
                if(rs.next()) {
                    String complaint = "multi return: " + locale + ":" + path;
                    logger.severe(complaint);
                    throw new InternalError(complaint);                    
                }
                rs.close();
                return rv;
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query data ("+tree + "/" + locale + ":" + path + "): " + SurveyMain.unchainSqlException(se));
                return null;
            }
        }
    }
	
    // new functions
	 public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) { } // read only.
	 public void putValueAtDPath(String distinguishingXPath, String value) { } // read only
	 public void removeValueAtDPath(String distinguishingXPath) {  }  // R/O
	
	 public String getValueAtDPath(String path) { return getValue(path); } // ?
	 public String getFullPathAtDPath(String path) { return path; } // ?
    
    
    public String getFullXPath(String path) {
        // TODO: verify that it's in the data
        return path; // for now: we don't have full xpaths.
    }
	public Comments getXpathComments() {
        return xpath_comments; // TODO: make this real.  For now, return this empty item.
    }
	public void setXpathComments(Comments path) {
        this.xpath_comments = xpath_comments;
    }
    public int size() {
        // query
        return -1; // TODO: 0
    }
    public void remove(String xpath) {
        // delete
        // TODO: 0
    }
    
    public Set getAvailableLocales() {
        File inFiles[] = getInFiles();
        Set s = new HashSet();
        if(inFiles == null) {
            return null;
//            throw new RuntimeException("Can't load CLDR data files from " + dir);
        }
        int nrInFiles = inFiles.length;
        
        for(int i=0;i<nrInFiles;i++) {
            String localeName = inFiles[i].getName();
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
                if(i != 0) {
                    s.add(localeName);
                }
            }
        }
        return s;
    }
    
    public Iterator iterator() {
        return keySet().iterator();
    }
    public Set keySet() {
        String locale = getLocaleID();
        synchronized (conn) {
            try {
                keySetStmt.setString(1,locale);
                ResultSet rs = keySetStmt.executeQuery();
                
                // TODO: is there a better way to map a ResultSet into a Set?
                Set s = new HashSet();
                while(rs.next()) {
                    s.add(xpt.getById(rs.getInt(1))); // xpath
                    //rs.getString(2); // origXpath
                }
                return Collections.unmodifiableSet(s);
                // TODO: 0
                // TODO: ???
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
        }
    }
    public XMLSource make(String localeID) {
        if(localeID == null) return null; // ???
        if(localeID.equals(CLDRFile.SUPPLEMENTAL_NAME)) return null;  // nothing, now.
        
        CLDRDBSource result = (CLDRDBSource)clone();
        result.setLocaleID(localeID);
        result.initConn(conn, factory); // set up connection & prepared statements. conn/factory may be set twice.
        return result;
    }
    private CLDRDBSource(CLDRFile.Factory nFactory, XPathTable nXpt) {
            factory = nFactory; 
            xpt = nXpt; 
    }
    public static CLDRDBSource createInstance(String theDir, XPathTable xpt, ULocale localeID, Connection conn, UserRegistry.User user) {
        CLDRFile.Factory afactory = CLDRFile.Factory.make(theDir,".*");
        CLDRDBSource result =  new CLDRDBSource(afactory, xpt);
        result.dir = theDir;
        result.setLocaleID(localeID.toString());
        result.initConn(conn, afactory);
        result.user = user;
        return result;
    }
    
	public Object clone() {
        try {
            CLDRDBSource result = (CLDRDBSource) super.clone();
            // copy junk
            result.xpath_comments = new Comments(); // TODO: make this real.
            // Copy SHARED things
            result.xpt = xpt; 
            result.dir = dir;
            result.user = user;
            result.conn = conn;  // gets set twice. but don't call initConn because other fields are still valid if it's just a clone.
            result.factory = factory;
            // do something here?
            return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}
	}
    // Lockable things
    public Object freeze() {
        locked = true;
        return this;
    }
    
    public java.sql.ResultSet listPrefix(String prefix) {
        String locale = getLocaleID();
        ResultSet rs = null;
        synchronized(conn) {
            try {
                queryXpathPrefixes.setString(1,prefix+"%");
                queryXpathPrefixes.setString(2,locale);
                rs = queryXpathPrefixes.executeQuery();
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
        }
        
        return rs;
    }
    
    public java.sql.ResultSet listForType(int xpath, String type) {
        return listForType(xpath, type, getLocaleID());
    }
    
    public java.sql.ResultSet listForType(int xpath, String type, String locale) {
        ResultSet rs = null;
        synchronized(conn) {
            try {
                queryTypeValues.setInt(1,xpath);
                queryTypeValues.setString(3,type);
                queryTypeValues.setString(2,locale);
                rs = queryTypeValues.executeQuery();
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query type ("+tree + "/" + locale + "/" + xpt.getById(xpath) + "/" + type +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
        }
        return rs;
    }
    protected File[] getInFiles() {
        // 1. get the list of input XML files
        FileFilter myFilter = new FileFilter() {
            public boolean accept(File f) {
                String n = f.getName();
                return(!f.isDirectory()
                       &&n.endsWith(".xml")
                       &&!n.startsWith("supplementalData") // not a locale
                       /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
            }
        };
        File baseDir = new File(dir);
        return baseDir.listFiles(myFilter);
    }
}
