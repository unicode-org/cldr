/*
 ******************************************************************************
 * Copyright (C) 2005-2006, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

//  Created by Steven R. Loomis on 31/10/2005.
//  an XMLSource which is implemented in a database

// TODO: if readonly (frozen), cache

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
//import org.unicode.cldr.util.XMLSource.Alias;
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
    
    public XPathTable xpt = null;         // XPathTable (shared) that is keyed to this database.
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
                                                    "value varchar(29000) not null, " +
                                                    "submitter INT, " +
                                                    "modtime TIMESTAMP )";
//            System.out.println(sql);
            s.execute(sql);
            sql = "create table " + CLDR_SRC + " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "locale varchar(20), " +
                                                    "tree varchar(20) NOT NULL, " +
                                                    "rev varchar(20), " +
                                                    "modtime TIMESTAMP, "+
                                                    "inactive INT)";
//            System.out.println(sql);
            s.execute(sql);
//            s.execute("CREATE UNIQUE INDEX unique_xpath on " + CLDR_DATA +"(xpath)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_qxpath on " + CLDR_DATA + "(locale,xpath)");

    /*
        superfluous indices.
            s.execute("CREATE INDEX "+CLDR_DATA+"_xpath on " + CLDR_DATA + "(xpath)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_txpath on " + CLDR_DATA + "(txpath)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_locale on " + CLDR_DATA + "(locale)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_origxpath on " + CLDR_DATA + "(origxpath)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_type on " + CLDR_DATA + "(type)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_submitter on " + CLDR_DATA + "(submitter)");
        */

            s.execute("CREATE INDEX "+CLDR_SRC+"_src on " + CLDR_SRC + "(locale,tree)");
            s.execute("CREATE INDEX "+CLDR_SRC+"_src_id on " + CLDR_SRC + "(id)");

            s.close();
            sconn.commit();
        }
        logger.info("CLDRDBSource DB: done.");
    }

    public class MyStatements { 
        public PreparedStatement insert = null;
        public PreparedStatement queryStmt = null;
        public PreparedStatement queryValue = null;
        public PreparedStatement queryXpathTypes = null;
        public PreparedStatement queryTypeValues = null;
        public PreparedStatement queryXpathPrefixes = null;
        public PreparedStatement keySet = null;
        public PreparedStatement keyASet = null;
        public PreparedStatement queryIdStmt = null;
        public PreparedStatement querySource = null;
        public PreparedStatement querySourceInfo = null;
        public PreparedStatement querySourceActives = null;
        public PreparedStatement insertSource = null;
        public PreparedStatement oxpathFromXpath = null;
    
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
         * ctor 
         */
        MyStatements(Connection conn) {
            insert = prepareStatement("insert",
                        "INSERT INTO " + CLDR_DATA +
                        " (xpath,locale,source,origxpath,value,type,alt_type,txpath,submitter) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)");
                        
                    // xpath - contains type, no draft
                    // origxpath - original xpath (full) - 
                    // txpath - tiny xpath  (no TYPE)

            queryXpathPrefixes = prepareStatement("queryXpathPrefixes",
                "select "+XPathTable.CLDR_XPATHS+".id from "+
                        XPathTable.CLDR_XPATHS+","+CLDR_DATA+" where "+CLDR_DATA+".xpath="+
                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ? AND "+CLDR_DATA+".locale=?");

            queryXpathTypes = prepareStatement("queryXpathTypes",
                "select " +CLDR_DATA+".type from "+
                        CLDR_DATA+","+XPathTable.CLDR_XPATHS+" where "+CLDR_DATA+".xpath="+
                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ?");

            oxpathFromXpath = prepareStatement("oxpathFromXpath",
                "select " +CLDR_DATA+".origxpath from "+CLDR_DATA+" where "+CLDR_DATA+".xpath=? AND "+CLDR_DATA+".locale=?");

            queryTypeValues = prepareStatement("queryTypeValues",
                "select "+CLDR_DATA+".value,"+CLDR_DATA+".alt_type,"+CLDR_DATA+".alt_proposed,"+CLDR_DATA+".submitter,"+CLDR_DATA+".xpath,"+CLDR_DATA+".origxpath " +
                        " from "+
                        CLDR_DATA+" where "+CLDR_DATA+".txpath=? AND "+CLDR_DATA+".locale=? AND "+CLDR_DATA+".type=?");

            queryValue = prepareStatement("queryValue",
                "SELECT value FROM " + CLDR_DATA +
                        " WHERE locale=? AND xpath=?"); // TODO: 1 need to be more specific! ! ! !
            keyASet = prepareStatement("keyASet",
            /*
                "SELECT "+CLDR_DATA+".xpath from "+
                        XPathTable.CLDR_XPATHS+","+CLDR_DATA+" where "+CLDR_DATA+".xpath="+
                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ? AND "+CLDR_DATA+".locale=?"
            */
//    "SELECT CLDR_DATA.xpath from CLDR_XPATHS,CLDR_DATA where CLDR_DATA.xpath=CLDR_XPATHS.id AND CLDR_XPATHS.xpath like '%/alias%' AND CLDR_DATA.locale=?"
    "SELECT CLDR_DATA.origxpath from CLDR_XPATHS,CLDR_DATA where CLDR_DATA.origxpath=CLDR_XPATHS.id AND CLDR_XPATHS.xpath like '%/alias%' AND CLDR_DATA.locale=?"
                        );


            keySet = prepareStatement("keySet",
                "SELECT " + "xpath FROM " + CLDR_DATA + // was origxpath
                        " WHERE locale=?"); // TODO: 1 need to be more specific!

            querySource = prepareStatement("querySource",
                "SELECT id,rev FROM " + CLDR_SRC + " where locale=? AND tree=? AND inactive IS NULL");

            querySourceInfo = prepareStatement("querySourceInfo",
                "SELECT rev FROM " + CLDR_SRC + " where id=?");
                
            querySourceActives = prepareStatement("querySourceActives",
                "SELECT id,locale,rev FROM " + CLDR_SRC + " where inactive IS NULL");
                
            insertSource = prepareStatement("insertSource",
                "INSERT INTO " + CLDR_SRC + " (locale,tree,rev,inactive) VALUES (?,?,?,null)");
        }
                
    }
    
    MyStatements stmts = null;
    
    /** 
     * Initialize with (a reference to the shared) Connection
     * set up statements, etc.
     *
     * Verify that the Source data is available.
     */
    private void initConn(Connection newConn, CLDRFile.Factory theFactory) {
        if(newConn != conn) {
            conn = newConn;
            if(stmts == null) {
                synchronized(conn) {
                    stmts = new MyStatements(newConn);
                }
            }
        }
        factory = theFactory;
        
        if(!loadAndValidate(getLocaleID(), null)) {
            throw new InternalError("Couldn't load and validate: " + getLocaleID());
        }
    }
    
    public int srcId = -1; 

    boolean loadAndValidate(String locale, WebContext forUser) {
            srcId = getSourceId(tree, locale);

            if(srcId != -1) { 
                return true;  // common case.
            }

        synchronized(xpt) {
            // double check..
            srcId = getSourceId(tree, locale);
            if(srcId != -1) { 
                return true;  // common case.
            }
            
            if(conn == null) {
                throw new InternalError("loadAndValidate: failed, no DB connection");
            }
            String rev=LDMLUtilities.getCVSVersion(dir, locale+".xml");
            srcId = setSourceId(tree, locale, rev); // TODO: we had better fill it in..
            synchronized(conn) {            
    //            logger.info("srcid: " + srcId);
                
                CLDRFile file = factory.make(locale, false, true);
                if(file == null) {
                    logger.severe("Couldn't load CLDRFile for " + locale);
                    return false ;
                }
                
                logger.info("loading rev: " + rev + " for " + dir + ":" + locale+".xml");
                // Now, start loading stuff in
                XPathParts xpp=new XPathParts(null,null);
                for (Iterator it = file.iterator(); it.hasNext();) {
                    String rawXpath = (String) it.next();
                    // Make it distinguished
                    String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);
                    if(!xpath.equals(rawXpath)) {
                        logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
                    }
                    String oxpath = file.getFullXPath(xpath);
                    if(!oxpath.equals(file.getFullXPath(rawXpath))) {
                        throw new InternalError("UHOH: oxpath and file.getFullXPath(raw) are different: " + oxpath + " VS. " + file.getFullXPath(rawXpath));
                    }
                    int xpid = xpt.getByXpath(xpath);
                    int oxpid = xpt.getByXpath(oxpath);
                    String value = file.getStringValue(xpath);
                    xpp.clear();
                    xpp.initialize(oxpath);
                    String lelement = xpp.getElement(-1);
                    /* all of these are always at the end */
                    String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
                    String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
        
                    /* special func to find this */
                    String eType = xpt.typeFromPathToTinyXpath(xpath, xpp);
                    String tinyXpath = xpp.toString();
                    
                    int txpid = xpt.getByXpath(tinyXpath);
                    
                    /*SRL*/ 
    //                System.out.println(xpath + " l: " + locale);
    //                System.out.println(" <- " + oxpath);
    //                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
    //                System.out.println(" => "+txpid+"#" + tinyXpath);
                      
                    try {
                        stmts.insert.setInt(1,xpid); // full
                        stmts.insert.setString(2,locale);
                        stmts.insert.setInt(3,srcId);
                        stmts.insert.setInt(4,oxpid); // Note: assumes XPIX = orig XPID! TODO: fix
                        stmts.insert.setString(5,value);
                        stmts.insert.setString(6,eType);
                        stmts.insert.setString(7,eAlt);
                        stmts.insert.setInt(8,txpid); // tiny
                        stmts.insert.setNull(9, java.sql.Types.INTEGER);

                        stmts.insert.execute();
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
        } // synch xpt
    }
    
    Hashtable srcHash = new Hashtable(); 
    
    public int getSourceId(String tree, String locale) {
        String key = tree + "_" + locale;
            synchronized (srcHash) {
                Integer r = null;
                r = (Integer)srcHash.get(key);
                if(r != null) return r.intValue(); // quick check
             synchronized(xpt) {
                synchronized (conn) {
                    try {
                        stmts.querySource.setString(1,locale);
                        stmts.querySource.setString(2,tree);
                        ResultSet rs = stmts.querySource.executeQuery();
                        if(!rs.next()) {
                            rs.close();
                            return -1;
                        }
                        int result = rs.getInt(1);
                        if(rs.next()) {
                            logger.severe("Source returns two results: " + tree + "/" + locale);
                            throw new InternalError("Issue with this Source: " + tree + "/" + locale);
                 //           return -1;
                        }
                        rs.close();
                        
                        r = new Integer(result);
                        srcHash.put(key,r); // add back to hash
    //logger.info(key + " - =" + r);
                        return result;
                    } catch(SQLException se) {
                        logger.severe("CLDRDBSource: Failed to find source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                        return -1;
                    }
                }
            }
        }
    }
    
    public String getSourceRevision() {
        return getSourceRevision(srcId);
    }
    
    public String getSourceRevision(int id) {
        if(id==-1) {
            return null;
        }
        String rev = null;
        synchronized (conn) {
            synchronized (xpt) {
                try {
                    stmts.querySourceInfo.setInt(1, id);
                    ResultSet rs = stmts.querySourceInfo.executeQuery();
                    if(rs.next()) {
                        rev = rs.getString(1); // rev
                        if(rs.next()) {
                            throw new InternalError("Duplicate source info for source " + id);
                        }
                    } 
                    // auto close
                    return rev;
                } catch (SQLException se) {
                    String what = ("CLDRDBSource: Failed to find source info ("+id +"): " + SurveyMain.unchainSqlException(se));
                    logger.severe(what);
                    throw new InternalError(what);
                }
            }
        }
    }
    
    int setSourceId(String tree, String locale, String rev) {
        synchronized (conn) {
            synchronized(xpt) {
                try {
                    stmts.insertSource.setString(1,locale);
                    stmts.insertSource.setString(2,tree);
                    stmts.insertSource.setString(3,rev);
                    if(!stmts.insertSource.execute()) {
                        conn.commit();
                        return getSourceId(tree, locale); // adds to hash
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
    }

    private void manageSourceUpdates_locale(WebContext ctx, SurveyMain sm, int id, String loc)
        throws SQLException
    {
        String mySql = ("DELETE from CLDR_DATA where source="+id+" AND submitter IS NULL");
        logger.severe("srcupdate: "+loc+" - "+ mySql);
        Statement s = conn.createStatement();
        int r = s.executeUpdate(mySql);
        ctx.println("<br>Deleting data from src " + id + " ... " + r + " rows.<br />");
        mySql = "UPDATE CLDR_SRC set inactive=1 WHERE id="+id;
        logger.severe("srcupdate:  "+loc+" - " + mySql);
        int j = s.executeUpdate(mySql);
        ctx.println(" Deactivating src: " + j + " rows<br />");
        logger.severe("srcupdate: " +loc + " - deleted " + r + " rows of data, and deactivated " + j + " rows of src ( id " + id +"). committing.. ");
        conn.commit();
        sm.lcr.invalidateLocale(loc); // force a reload.
    }
    
    public void manageSourceUpdates(WebContext ctx, SurveyMain sm) {
     //       querySourceActives = prepareStatement("querySourceActives",
     //           "SELECT id,locale,rev FROM " + CLDR_SRC + " where inactive IS NULL");
        String what = ctx.field("src_update");
        boolean updAll = what.equals("all_locs");
        ctx.println("<h4>Source Update Manager</h4>");
        synchronized (conn) {
            synchronized(xpt) {
                try {
                    boolean hadDiffs = false;
                    ResultSet rs = stmts.querySourceActives.executeQuery();
                    ctx.println("<ul>");
                    while(rs.next()) {
                        int id = rs.getInt(1);
                        String loc = rs.getString(2);
                        String rev = rs.getString(3);
                        String disk = LDMLUtilities.getCVSVersion(dir, loc+".xml");
                        ctx.println("<li> <tt>[#"+id+"]" +loc + "</tt>:  db="+rev+" ");
                        if(rev == null )  {
                            rev = "null";
                        }
                        if(disk == null) {
                            disk = "null";
                        }
                        if(rev.equals(disk)) {
                            ctx.println(" - latest");
                        } else {
                            hadDiffs = true;
                            ctx.println("<span class='dashbox'> != disk="+disk+ " </span> ");
                            WebContext subCtx = new WebContext(ctx);
                            subCtx.addQuery("src_update",loc);
                            // ...
                            if(updAll || what.equals(loc)) {
                                manageSourceUpdates_locale(ctx,sm,id,loc);
                            } else {
                                ctx.println("<a href='"+subCtx.url()+"'>Update "+loc+"</a>");
                            }
                        }
                        ctx.println("</li>");
                    }
                    if(hadDiffs) {
                            WebContext subCtx = new WebContext(ctx);
                            subCtx.addQuery("src_update","all_locs");
                            ctx.println("<li><b><a href='"+subCtx.url()+"'>Update ALL</b></li>");
                    }
                    ctx.println("</ul>");
                } catch(SQLException se) {
                    String complaint = ("CLDRDBSource: err in manageSourceUpdates["+what+"] ("+tree + "/" + "*" +"): " + SurveyMain.unchainSqlException(se));
                    logger.severe(complaint);
                    ctx.println("<hr /><pre>" + complaint + "</pre><br />");
                    return;
                }
            }
        }
    }
    
	protected void putFullPath(String distinguishingXPath, String fullxpath) {
        throw new InternalError("read-only");
        // TODO: 0
    }
    
	protected void putValue(String distinguishingXPath, String value) {
        throw new InternalError("read-only");
        // TODO: 0
    }
    
    public boolean hasValueAtDPath(String path) // d path
    {
        if(conn == null) {
            throw new InternalError("No DB connection!");
        }
    
        String locale = getLocaleID();
//logger.info(locale + ":" + path);
//        synchronized (conn) {
            try {
                stmts.queryValue.setString(1,locale);
                stmts.queryValue.setInt(2,xpt.getByXpath(path)); // TODO: 2 more specificity
                ResultSet rs = stmts.queryValue.executeQuery();
                if(rs.next()) {
                    return true;
                } else {
                    return false;
                }
//                rs.close();
//logger.info(locale + ":" + path+" -> " + rv);
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to check data ("+tree + "/" + locale + ":" + path + "): " + SurveyMain.unchainSqlException(se));
                return false;
            }
//        }
    }
    
    public String getValueAtDPath(String path) // D path
    {
        if(conn == null) {
            throw new InternalError("No DB connection!");
        }
    
        String locale = getLocaleID();
//logger.info(locale + ":" + path);
//        synchronized (conn) {
            try {
                stmts.queryValue.setString(1,locale);
                stmts.queryValue.setInt(2,xpt.getByXpath(path)); // TODO: 2 more specificity
                ResultSet rs = stmts.queryValue.executeQuery();
                String rv;
                if(!rs.next()) {
                    rs.close();
//                    if(true && locale.equals("root")) { // TODO: check this later.
//                        throw new InternalError("No value at root=" + locale + " path " + path);                    
//                    }                    
//logger.info(locale + ":" + path+" -> null");
                  return null;
                }
                rv = rs.getString(1);
                if(rs.next()) {
                    String complaint = "multi return: " + locale + ":" + path;
                    logger.severe(complaint);
                    throw new InternalError(complaint);                    
                }
                rs.close();
//logger.info(locale + ":" + path+" -> " + rv);
                return rv;
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query data ("+tree + "/" + locale + ":" + path + "): " + SurveyMain.unchainSqlException(se));
                return null;
            }
//        }
    }
	
    // new functions
	 public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) { 
                throw new InternalError("read-only");
     } // read only.
	 public void putValueAtDPath(String distinguishingXPath, String value) { 
                throw new InternalError("read-only");
     } // read only
	 public void removeValueAtDPath(String distinguishingXPath) {  
                throw new InternalError("read-only");
    }  // R/O
	    
    /*
     * @param path cleaned path
     */
    public String getFullPathAtDPath(String path) {
        String locale = getLocaleID();
        int pathid = xpt.getByXpath(path); // note: want this to fail, or it will fill xpt with trash.
//        if(nid < 0) {
//                return path;
//        }
//        synchronized (conn) {
            try {
                stmts.oxpathFromXpath.setInt(1,pathid);
                stmts.oxpathFromXpath.setString(2,locale);
                ResultSet rs = stmts.oxpathFromXpath.executeQuery();
                if(!rs.next()) {
                    rs.close();
                    logger.severe("gfx not found, falling back: " + locale + "/" + path);
                    return path; // not found - should be null?
                }
                int result = rs.getInt(1);
                if(rs.next()) {
                    logger.severe("gfx returns two results: " + locale + "/" + path);
                    // fail?? what?
                }
                rs.close();
                return xpt.getById(result);
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to find source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return path; //? should be null?
            }
//        }
    }
    
    
	public Comments getXpathComments() {
        return xpath_comments; // TODO: make this real.  For now, return this empty item.
    }
	public void setXpathComments(Comments path) {
        this.xpath_comments = xpath_comments;
    }
    
    public int size() {
        throw new InternalError("not implemented yet");
        // query
  //      return -1; // TODO: 0
    }
    public void remove(String xpath) {
        throw new InternalError("read-only");
        // delete
        // TODO: 0
    }
    
    public Set getAvailableLocales() {
        // TODO: optimize
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
//com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
        Iterator i =  oldKeySet().iterator();
//logger.info(et + " for iterator on " + getLocaleID());
        return i;
    }


    public Iterator iterator(String prefix) {
//com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
        Iterator i =  prefixKeySet(prefix).iterator();
//logger.info(et + " for iterator on " + getLocaleID());
        return i;
    }

    /**
     * @deprecated
     * TODO: rewrite as iterator
     */
    private Set oldKeySet() {
        String locale = getLocaleID();
//        synchronized (conn) {
            try {
                stmts.keySet.setString(1,locale);
                ResultSet rs = stmts.keySet.executeQuery();
                
                // TODO: is there a better way to map a ResultSet into a Set?
                Set s = new HashSet();
//                System.err.println("@tlh: " + "BEGIN");
                while(rs.next()) {
                    String xpath = (xpt.getById(rs.getInt(1)));
/*                    if(-1!=xpath.indexOf("tlh")) {
                        xpath = xpath.replaceAll("\\[@draft=\"true\"\\]","");
                        System.err.println("@tlh: " + xpath);
                    }
                    */
                    s.add(xpath); // xpath
                    //rs.getString(2); // origXpath
                }
//                System.err.println("@tlh: " + "END");
                return Collections.unmodifiableSet(s);
                // TODO: 0
                // TODO: ???
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
//        }
    }

    private Set prefixKeySet(String prefix) {
//        String locale = getLocaleID();
//        synchronized (conn) {
            try {
//                stmts.keySet.setString(1,locale);
                ResultSet rs = getPrefixKeySet(prefix);
                
                // TODO: is there a better way to map a ResultSet into a Set?
                Set s = new HashSet();
//                System.err.println("@tlh: " + "BEGIN");
                while(rs.next()) {
                    String xpath = (xpt.getById(rs.getInt(1)));
                    if(-1!=xpath.indexOf("tlh")) {
                        xpath = xpath.replaceAll("\\[@draft=\"true\"\\]","");
//                        System.err.println("@tlh: " + xpath);
                    }
                    s.add(xpath); // xpath
                    //rs.getString(2); // origXpath
                }
//                System.err.println("@tlh: " + "END");
                return Collections.unmodifiableSet(s);
                // TODO: 0
                // TODO: ???
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + getLocaleID() +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
//        }
    }

    

    private Hashtable aliasTable = new Hashtable();
    
	public List addAliases(List output) {
        String locale = getLocaleID();
//        com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
        List aList = getAliases();
//        logger.info(et + " for getAlias on " + locale);
        output.addAll(aList);
        return output;
    }

	public List getAliases() {
        String locale = getLocaleID();
        List output = null;
        synchronized(aliasTable) {
            output = (List)aliasTable.get(locale);
            if(null==output) {
                output = new ArrayList();
                try {
                    //  stmts.keyASet.setString(1,"%/alias");
                    stmts.keyASet.setString(1,locale);
                    ResultSet rs = stmts.keyASet.executeQuery();
                    
                    // TODO: is there a better way to map a ResultSet into a Set?
                    Set s = new HashSet();
                    while(rs.next()) {
                        String fullPath = xpt.getById(rs.getInt(1));
                        // if(path.indexOf("/alias")<0) { throw new InternalError("aliasIteratorBroken: " + path); }
                        //     String fullPath = getFullPathAtDPath(path);
                        //System.out.println("oa: " + locale +" : " + path + " - " + fullPath);
                        Alias temp = XMLSource.Alias.make(fullPath);
                        if (temp == null) continue;
                        //System.out.println("aa: " + path + " - " + fullPath + " -> " + temp.toString());
                        output.add(temp);
                    }
                    aliasTable.put(locale,output);
                    return output;
                    // TODO: 0
                    // TODO: ???
                } catch(SQLException se) {
                    logger.severe("CLDRDBSource: Failed to query A source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                    return null;
                }
            }
            return output;
        }
    }
    
    public XMLSource make(String localeID) {
        if(localeID == null) return null; // ???
        if(localeID.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
            XMLSource msource = new CLDRFile.SimpleXMLSource(factory, localeID).make(localeID);
//            System.err.println("Getting simpleXMLSource for " + localeID);
            return msource; 
        }
        
        CLDRDBSource result = (CLDRDBSource)clone();
        if(!localeID.equals(result.getLocaleID())) {
            result.setLocaleID(localeID);
            result.initConn(conn, factory); // set up connection & prepared statements. conn/factory may be set twice.
        }
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
            result.stmts = stmts;
            result.srcHash = srcHash;
            result.aliasTable = aliasTable;
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
    
    
    // TODO: remove this, implement as iterator( stringPrefix)
    public java.sql.ResultSet getPrefixKeySet(String prefix) {
        String locale = getLocaleID();
        ResultSet rs = null;
        synchronized(conn) {
            try {
                stmts.queryXpathPrefixes.setString(1,prefix+"%");
                stmts.queryXpathPrefixes.setString(2,locale);
                rs = stmts.queryXpathPrefixes.executeQuery();
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to getPrefixKeySet ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
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
                stmts.queryTypeValues.setInt(1,xpath);
                stmts.queryTypeValues.setString(3,type);
                stmts.queryTypeValues.setString(2,locale);
                rs = stmts.queryTypeValues.executeQuery();
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query type ("+tree + "/" + locale + "/" + xpt.getById(xpath) + "/" + type +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
        }
        return rs;
    }
    private File[] getInFiles() {
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
    
    /**
     * Add new data to the next sequentially available slot. 
     * This does perform a linear search, however it is only active when we are adding data, so should not
     * be an issue. 
     * returns: the entire altProposed which succeeded or NULL/throw for failure.
     */
    public String addDataToNextSlot(CLDRFile file, String locale, String fullXpathMinusAlt, 
                                    String altType, String altProposedPrefix, int submitterId, String value, String refs) {
//                                    if(1==1) {throw new InternalError("Sorry, adding data is temporarily disabled whilst some xpath bugs are ironed out..<p> please click Back in your browser to continue. ");}
        XPathParts xpp = new XPathParts(null,null);
        // prepare for slot check
        for(int slot=1;slot<100;slot++) {
            String altProposed = null;
            String alt = null;
            String altTag = null;
            if(altProposedPrefix != null) {
                altProposed = altProposedPrefix+slot; // proposed-u123-4 
                alt = LDMLUtilities.formatAlt(altType, altProposed);
                altTag =  "[@alt=\"" + alt + "\"]";
            } else {
                // no alt
                altTag = "";
            }
            //            String rawXpath = fullXpathMinusAlt + "[@alt='" + alt + "']";
            String refStr = "";
            if(refs.length()!=0) {
                refStr = "[@references=\""+refs+"\"]";
            }
            String rawXpath = fullXpathMinusAlt + altTag + refStr; // refstr will get removed
            logger.info("addDataToNextSlot:  rawXpath = " + rawXpath);
            String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);
            if(!xpath.equals(rawXpath)) {
                logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
            }
            String oxpath = xpath+refStr+"[@draft=\"true\"]";
            int xpid = xpt.getByXpath(xpath);
            
            // Check to see if this slot is in use.
            synchronized(conn) {
                try {
                    stmts.queryValue.setString(1,locale);
                    stmts.queryValue.setInt(2,xpid);
                    ResultSet rs = stmts.queryValue.executeQuery();
                    if(rs.next()) {
                        // already taken..
                        logger.info("Taken: " + altProposed);
                        rs.close();
                        continue;
                    }
                    rs.close();
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't search for empty slot " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
            }
            
            
            
            int oxpid = xpt.getByXpath(oxpath);
            xpp.clear();
            xpp.initialize(oxpath);
            String lelement = xpp.getElement(-1);
            /* all of these are always at the end */
            String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
            String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
            
            /* special func to find this */
            String eType = xpt.typeFromPathToTinyXpath(xpath, xpp);
            String tinyXpath = xpp.toString();
            
            int txpid = xpt.getByXpath(tinyXpath);
            
            /*SRL*/ 
            //                System.out.println(xpath + " l: " + locale);
            //                System.out.println(" <- " + oxpath);
            //                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
            //                System.out.println(" => "+txpid+"#" + tinyXpath);
            
            synchronized(conn) {
                try {
                    stmts.insert.setInt(1,xpid); // full
                    stmts.insert.setString(2,locale);
                    stmts.insert.setInt(3,srcId); // assumes homogenous srcId
                    stmts.insert.setInt(4,oxpid);
                    stmts.insert.setString(5,value);
                    stmts.insert.setString(6,eType);
                    stmts.insert.setString(7,eAlt);
                    stmts.insert.setInt(8,txpid); // tiny
                    stmts.insert.setInt(9,submitterId); // submitter
                    stmts.insert.execute();
                    
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't insert " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
                
                try{
                    conn.commit();
                    return altProposed; // success
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't commit " + locale + ":" + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
            }
            
        } // end for
        return null; // couldn't find a slot..
    }


    /**
     * Add new data to the next sequentially available slot. 
     * This does perform a linear search, however it is only active when we are adding data, so should not
     * be an issue. 
     * returns: the entire altProposed which succeeded or NULL/throw for failure.
     */
    public String addReferenceToNextSlot(CLDRFile file, String locale, int submitterId, String value, String uri) {
        XPathParts xpp = new XPathParts(null,null);
        String uristr  ="";
        if(uri.length()>0) {
            uristr =  "[@uri=\""+uri+"\"]";
        }
        // prepare for slot check
        for(int slot=1;slot<100;slot++) {
            String type = "RP"+slot; // proposed-u123-4 
//            String alt = LDMLUtilities.formatAlt(altType, altProposed);
            //            String rawXpath = fullXpathMinusAlt + "[@type='" + alt + "'][@draft='true']";
            String rawXpath = "//ldml/references/reference[@type=\""+type+"\"]"+uristr+"[@draft=\"true\"]";
            logger.info("addDataToNextSlot:  rawXpath = " + rawXpath);
            String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);
            if(!xpath.equals(rawXpath)) {
                logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
            }
            String oxpath = xpath+uristr+"[@draft=\"true\"]";
            int xpid = xpt.getByXpath(xpath);
            
            // Check to see if this slot is in use.
            synchronized(conn) {
                try {
                    stmts.queryValue.setString(1,locale);
                    stmts.queryValue.setInt(2,xpid);
                    ResultSet rs = stmts.queryValue.executeQuery();
                    if(rs.next()) {
                        // already taken..
                        logger.info("Ref Taken: " + type);
                        rs.close();
                        continue;
                    }
                    rs.close();
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't search for empty slot " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
            }
            
            // some of the following may not need to be dynamic, however going to leave it alone for consistency
            int oxpid = xpt.getByXpath(oxpath);
            xpp.clear();
            xpp.initialize(oxpath);
            String lelement = xpp.getElement(-1);
            /* all of these are always at the end */
            String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
            String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
            
            /* special func to find this */
            String eType = type;
            String tinyXpath = xpp.toString();
            
            int txpid = xpt.getByXpath(tinyXpath);
            
            /*SRL*/ 
            //                System.out.println(xpath + " l: " + locale);
            //                System.out.println(" <- " + oxpath);
            //                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
            //                System.out.println(" => "+txpid+"#" + tinyXpath);
            
            synchronized(conn) {
                try {
                    stmts.insert.setInt(1,xpid); // full
                    stmts.insert.setString(2,locale);
                    stmts.insert.setInt(3,srcId); // assumes homogenous srcId
                    stmts.insert.setInt(4,oxpid);
                    stmts.insert.setString(5,value);
                    stmts.insert.setString(6,eType);
                    stmts.insert.setString(7,eAlt);
                    stmts.insert.setInt(8,txpid); // tiny
                    stmts.insert.setInt(9,submitterId); // submitter
                    stmts.insert.execute();
                    
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't insert " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
                
                try{
                    conn.commit();
                    return type; // success
                } catch(SQLException se) {
                    String complaint = "CLDRDBSource: Couldn't commit " + locale + ":" + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    throw new InternalError(complaint);
                }
            }
            
        } // end for
        return null; // couldn't find a slot..
    }

}