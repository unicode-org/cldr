/*
 ******************************************************************************
 * Copyright (C) 2005-2008, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

//  Created by Steven R. Loomis on 31/10/2005.
//
//  an XMLSource which is implemented in a database

// TODO: if readonly (frozen), cache

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;

// JDBC imports
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

// CLDR imports
import org.unicode.cldr.util.*;
//import org.unicode.cldr.util.XMLSource.Alias;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.CLDRFileCache.CacheableXMLSource;
import org.unicode.cldr.icu.LDMLConstants;

// ICU
import com.ibm.icu.util.ULocale;

public class CLDRDBSourceFactory {
    /**
     * @author srl
     *
     */
    public class MuxedSource extends XMLSource {
        private CLDRLocale locale;
        
        private XMLSource dbSource = null;
        private XMLSource cachedSource = null;
        private boolean finalData;
        /**
         * 
         */
        public MuxedSource(CLDRLocale locale, boolean finalData) {
            this.setLocaleID(locale.toString());
            this.locale = locale;
            this.finalData = finalData;
            this.dbSource = rootDbSource.make(locale.toString());
            this.cachedSource = cache.getSource(locale, finalData);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getAvailableLocales()
         */
        @Override
        public Set getAvailableLocales() {
            return dbSource.getAvailableLocales();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            if(false) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " = " + cachedSource.getFullPathAtDPath(path));    }
            return cachedSource.getFullPathAtDPath(path);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getWinningPath(String path) {
          //   if(path.indexOf("=\"TR")>0&&path.indexOf("symbol")>0) { /*srl*/
          //      if(true) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " WP= " + cachedSource.getWinningPath(path));    }
          //  } */
            if(false) {  System.err.println("NN: F["+path+"] @"+getLocaleID() + " = " + cachedSource.getWinningPath(path));    }
            return cachedSource.getWinningPath(path);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getSupplementalDirectory()
         */
        @Override
        public File getSupplementalDirectory() {
            // TODO Auto-generated method stub
            return dbSource.getSupplementalDirectory();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            //if(path.contains("gregorian")) throw new InternalError("Who wants to know?");
            if(false) {  System.err.println("NN: ["+path+"] @"+getLocaleID() + " = " + cachedSource.getValueAtDPath(path));    }
            return cachedSource.getValueAtDPath(path);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            // TODO Auto-generated method stub
            return cachedSource.getXpathComments();
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            // TODO Auto-generated method stub
          if(false) {  System.err.println("NN: @"+getLocaleID());
            for(Iterator i = (Iterator)cachedSource.iterator();i.hasNext();) {
                System.err.println("// "+i.next().toString());
            } }
            return cachedSource.iterator();
        }

        public Iterator<String> iterator(String str) {
            if(false) {  System.err.println("NN: ["+str+"] @"+getLocaleID());
            for(Iterator i = (Iterator)cachedSource.iterator(str);i.hasNext();) {
                System.err.println("// "+i.next().toString());
            } }
            return cachedSource.iterator(str);
        }
//
        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#make(java.lang.String)
         */
        @Override
        public XMLSource make(String localeID) {
        	if(localeID.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
        		return dbSource.make(localeID); // will fal through to raw files
        	} else {
        		return getMuxedInstance(CLDRLocale.getInstance(localeID));
        	}
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            cachedSource.putFullPathAtDPath(distinguishingXPath, fullxpath);
            dbSource.putFullPathAtDPath(distinguishingXPath, fullxpath);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String, java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            // TODO Auto-generated method stub
            cachedSource.putValueAtDPath(distinguishingXPath, value);
            dbSource.putValueAtDPath(distinguishingXPath, value);
        }
        
        public String putValueAtPath(String x, String v) {
            cachedSource.putValueAtPath(x, v);
            return dbSource.putValueAtPath(x, v);
        }
        
        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            cachedSource.removeValueAtDPath(distinguishingXPath);
            dbSource.removeValueAtDPath(distinguishingXPath);
        }

        /* (non-Javadoc)
         * @see org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr.util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            // TODO Auto-generated method stub
            throw new InternalError("not imp");

        }

        /**
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        public Object freeze() {
            // TODO Auto-generated method stub
//            locked = true;
//            return this;
            throw new InternalError("freeze not suported. [clone and lock?]");
        }

    }

    private static final boolean SHOW_TIMES=false;
    private static final boolean SHOW_DEBUG=false;
    private static final boolean TRACE_CONN=false;
    
    /**
     * the logger to use, from SurveyMain
     **/
    private static Logger logger;

    /**
     * TODO: 0 make private
     * factory for producing XMLFiles that go with the original source xml data.
     */
    public CLDRFile.Factory  rawXmlFactory = null; 
    
    CLDRFileCache cache = null; 
    /**
     * location of LDML data.
     */
    private String dir = null;
    
    // DB things
 
    /**
     * The table containing CLDR data
     */
    public static final String CLDR_DATA = "cldr_data";
    
    /**
     * The table containing the Sources (i.e. list of LDML files
     */
    public static final String CLDR_SRC = "cldr_src";
    
    /**
     * For now, we are only concerned with the main/ tree
     */
    private String tree = "main";
    
    /**
     * XPathTable (shared) that is keyed to this database.
     */
    public XPathTable xpt = null;         
    
    /**
     * A referece back to the main SurveyMain. (for xpt, etc)
     */
    static SurveyMain sm = null;
    
    /**
     * the SQL database connection.
     */
    protected Connection conn = null;
    
    protected XMLSource rootDbSource = null;
    protected XMLSource rootDbSourceV = null;
    private List<CLDRLocale> needUpdate = new ArrayList<CLDRLocale>();
    File cacheDir = null;
    boolean vetterReady = false;
    
    CLDRDBSourceFactory(SurveyMain sm, String theDir, Logger xlogger, File cacheDir) throws SQLException {
        this.xpt = sm.xpt;
        this.dir = theDir;
        Connection sconn = sm.getDBConnection();
        CLDRDBSourceFactory.sm = sm;
        logger = xlogger; // set static
        setupDB(sconn);
        // initconn done later, in vetterReady
        this.cacheDir = cacheDir;
        SurveyMain.closeDBConnection(sconn);
    }
    
    public void vetterReady() {
        System.err.println("DBSRCFAC: processing vetterReady()... initializing connection");
        Connection sconn = sm.getDBConnection();
        CLDRFile.Factory afactory = CLDRFile.SimpleFactory.make(this.dir,".*");
        this.initConn(sconn, afactory);
        System.err.println("DBSRCFAC: processing vetterReady()...");
        rootDbSourceV = new CLDRDBSource(CLDRLocale.ROOT, true);
        rootDbSource = new CLDRDBSource(CLDRLocale.ROOT, false);
        cache= new CLDRFileCache(rootDbSource, rootDbSourceV, cacheDir, sm);

        vetterReady = true;
        update();
    }
    
    /**
     * Mark a locale as needing update.
     * @param loc
     */
    public void needUpdate(CLDRLocale loc) {
    	if(!needUpdate.contains(loc)) 
    	needUpdate.add(loc);
    }

    
    /**
     * Execute deferred updates. Call this at a high level when all updates are complete.
     */
    public int update() {
    	int n = 0;
        synchronized(sm.vet.conn) {
            for(CLDRLocale l : needUpdate) {
            	n++;
                System.err.println("CLDRDBSRCFAC: executing deferred update of " + l +"("+needUpdate.size()+" on queue)");
                sm.vet.updateResults(l);
                synchronized(this) {
	                XMLSource inst = getInstance(l,false);
	                // TODO: fix broken layering
	                XMLSource cached = ((MuxedSource)inst).cachedSource;
	                ((CacheableXMLSource)cached).reloadWinning(((MuxedSource)inst).dbSource);
	                ((CacheableXMLSource)cached).save();
                }
            }
            needUpdate.clear();
         }
        return n;
    }

    XMLSource getInstance(CLDRLocale locale) {
        return getInstance(locale, false);
     }
    
    /**
     * The Muxed sources automatically are cached on read, and update on write.
     */
    Map<CLDRLocale, XMLSource> mux = new HashMap<CLDRLocale, XMLSource>();
    
    public XMLSource  getInstance(CLDRLocale locale, boolean finalData) {
        if(finalData == true) {
            return rootDbSourceV.make(locale.toString());
        } else {
            return getMuxedInstance(locale);
        }
     }
    
    public XMLSource getMuxedInstance(CLDRLocale locale) {
    	//synchronized(mux) {
	        XMLSource src = mux.get(locale);
	        if(src == null) {
	        	CLDRLocale parent = locale.getParent();
	        	if(parent != null) {
				XMLSource ignored = mux.get(parent);
				if(ignored == null) {
	        		  System.err.println("First loading parent locale "+locale.toString()+"->" + parent.toString());
	        		  ignored = getMuxedInstance(parent);
	               }
	        	}
	            src = new MuxedSource(locale, false);
	            mux.put(locale, src);
	        }
	        return src;
    	//}
    }
    
    /** 
     * called once (at DB setup time) to initialize the database.
     * @param sconn the database connection to be used for initial table creation
     */
    public void setupDB(Connection sconn) throws SQLException
    {
        boolean isNew = !sm.hasTable(sconn, CLDR_DATA);
        if(!isNew) {
            return; // nothing to setup
        }
        logger.info("CLDRDBSource DB: initializing...");
        synchronized(sconn) {
            String sql; // this points to 
            Statement s = sconn.createStatement();
            
            sql = "create table " + CLDR_DATA + " (id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                "xpath INT not null, " + // normal
                "txpath INT not null, " + // tiny
                "locale varchar(20), " +
                "source INT, " +
                "origxpath INT not null, " + // full
                "alt_proposed varchar(50), " +
                "alt_type varchar(50), " +
                "type varchar(50), " +
                "value "+sm.DB_SQL_UNICODE+" not null, " +
                "submitter INT, " +
                "modtime TIMESTAMP, " +
                // new additions, April 2006
                "base_xpath INT NOT NULL "+sm.DB_SQL_WITHDEFAULT+" -1 " + // alter table CLDR_DATA add column base_xpath INT NOT NULL WITH DEFAULT -1
                " )";
            //            System.out.println(sql);
            s.execute(sql);
            sql = "create table " + CLDR_SRC + " (id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                "locale varchar(20), " +
                "tree varchar(20) NOT NULL, " +
                "rev varchar(20), " +
                "modtime TIMESTAMP, "+
                "inactive INT)";
            // System.out.println(sql);
            s.execute(sql);
            // s.execute("CREATE UNIQUE INDEX unique_xpath on " + CLDR_DATA +"(xpath)");
            s.execute("CREATE INDEX "+CLDR_DATA+"_qxpath on " + CLDR_DATA + "(locale,xpath)");
            // New for April 2006.
            s.execute("create INDEX "+CLDR_DATA+"_qbxpath on "+CLDR_DATA+"(locale,base_xpath)");
            s.execute("CREATE INDEX "+CLDR_SRC+"_src on " + CLDR_SRC + "(locale,tree)");
            s.execute("CREATE INDEX "+CLDR_SRC+"_src_id on " + CLDR_SRC + "(id)");
            s.close();
            sconn.commit();
        }
        logger.info("CLDRDBSource DB: done.");
    }
    
    /** 
     * this inner class contains a link to all of the prepared statements needed by the CLDRDBSource.
     * it may be shared by certain CLDRDBSources, or lazy initialized.
     **/
    public class MyStatements { 
        public PreparedStatement insert = null;
        public PreparedStatement queryStmt = null;
        public PreparedStatement queryValue = null;
        public PreparedStatement queryXpathTypes = null;
        public PreparedStatement queryTypeValues = null;
        public PreparedStatement queryXpathPrefixes = null;
        public PreparedStatement keySet = null;
        public PreparedStatement keyASet = null;
        public PreparedStatement keyVettingSet = null;
        public PreparedStatement oxpathFromVetXpath = null;
        public PreparedStatement keyUnconfirmedSet = null;
        public PreparedStatement queryVetValue = null;
        public PreparedStatement queryVetXpath = null;
        public PreparedStatement queryIdStmt = null;
        public PreparedStatement querySource = null;
        public PreparedStatement querySourceInfo = null;
        public PreparedStatement querySourceActives = null;
        public PreparedStatement insertSource = null;
        public PreparedStatement oxpathFromXpath = null;
        public PreparedStatement keyNoVotesSet = null;
        public PreparedStatement removeItem = null;
        public PreparedStatement getSubmitterId = null;
        
        /**
         * called to initialize one of the preparedstatement fields
         * @param name the shortname of the PreparedStatement field. For debugging.
         * @param sql the SQL to initialize the statement
         * @return the new prepared statement, or throws an error..
         */
        public PreparedStatement prepareStatement(String name, String sql) {
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
         * Constructor for the MyStatements 
         */
        MyStatements(Connection conn) {
            insert = prepareStatement("insert",
                                      "INSERT INTO " + CLDR_DATA +
                                      " (xpath,locale,source,origxpath,value,type,alt_type,txpath,submitter,base_xpath,modtime) " +
                                      "VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)");
            
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
                                          " WHERE locale=? AND xpath=?"); 
            

            queryVetValue = prepareStatement("queryVetValue",
                                             "SELECT "+CLDR_DATA+".value FROM "+Vetting.CLDR_OUTPUT+"," + CLDR_DATA +
                                             " WHERE "+Vetting.CLDR_OUTPUT+".locale=? AND "+Vetting.CLDR_OUTPUT+".output_xpath=? AND "
                                             +" ("+Vetting.CLDR_OUTPUT+".locale="+CLDR_DATA+".locale) AND ("+Vetting.CLDR_OUTPUT+".data_xpath="+CLDR_DATA+".xpath)"); 

            oxpathFromVetXpath = prepareStatement("oxpathFromVetXpath",
                                             "SELECT "+Vetting.CLDR_OUTPUT+".output_full_xpath FROM "+Vetting.CLDR_OUTPUT+" " +
                                             " WHERE "+Vetting.CLDR_OUTPUT+".locale=? AND "+Vetting.CLDR_OUTPUT+".output_xpath=? "); 

            queryVetXpath = prepareStatement("queryVetXpath",
                                             "SELECT "+Vetting.CLDR_RESULT+".result_xpath FROM "+Vetting.CLDR_RESULT+" " +
                                             " WHERE "+Vetting.CLDR_RESULT+".locale=? AND "+Vetting.CLDR_RESULT+".base_xpath=? "); 

            //keyVettingSet = prepareStatement("keyVettingSet",
              //                               "SELECT output_xpath from "+Vetting.CLDR_OUTPUT+" where locale=?" ); // wow, that is pretty straightforward...
            keyVettingSet = prepareStatement("keyVettingSet",
                                             "SELECT "+Vetting.CLDR_OUTPUT+".output_xpath from "+Vetting.CLDR_OUTPUT+" where "+Vetting.CLDR_OUTPUT+".locale=? AND EXISTS "+
                                                    " ( SELECT * from "+CLDR_DATA+" where "+CLDR_DATA+".locale="+Vetting.CLDR_OUTPUT+".locale AND "+CLDR_DATA+".xpath="+Vetting.CLDR_OUTPUT+".data_xpath )");
                                            
            keyASet = prepareStatement("keyASet",
                                       /*
                                        "SELECT "+CLDR_DATA+".xpath from "+
                                        XPathTable.CLDR_XPATHS+","+CLDR_DATA+" where "+CLDR_DATA+".xpath="+
                                        XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like ? AND "+CLDR_DATA+".locale=?"
                                        */
                                       //    "SELECT "+CLDR_DATA+".xpath from CLDR_XPATHS,CLDR_DATA where CLDR_DATA.xpath=CLDR_XPATHS.id AND CLDR_XPATHS.xpath like '%/alias%' AND CLDR_DATA.locale=?"
                                       "SELECT "+CLDR_DATA+".origxpath from "+XPathTable.CLDR_XPATHS+","+CLDR_DATA+" where "+CLDR_DATA+".origxpath="+XPathTable.CLDR_XPATHS+".id AND "+XPathTable.CLDR_XPATHS+".xpath like '%/alias%' AND "+CLDR_DATA+".locale=?"
                                       );
            
            
            keySet = prepareStatement("keySet",
                                      "SELECT " + "xpath FROM " + CLDR_DATA + // was origxpath
                                      " WHERE locale=?"); 
            keyUnconfirmedSet = prepareStatement("keyUnconfirmedSet",
                                                 "select distinct "+Vetting.CLDR_VET+".vote_xpath from "+Vetting.CLDR_VET+" where "+
                                                 Vetting.CLDR_VET+".vote_xpath!=-1 AND "+Vetting.CLDR_VET+
                                                 ".locale=? AND NOT EXISTS ( SELECT "+Vetting.CLDR_RESULT+
                                                 ".result_xpath from "+Vetting.CLDR_RESULT+" where "+
                                                 Vetting.CLDR_RESULT+".result_xpath="+Vetting.CLDR_VET+".vote_xpath and "+
                                                 Vetting.CLDR_RESULT+".locale="+Vetting.CLDR_VET+".locale AND "+
                                                 Vetting.CLDR_RESULT+".type>="+Vetting.RES_ADMIN+") AND NOT EXISTS ( SELECT "+
                                                 Vetting.CLDR_RESULT+".base_xpath from "+Vetting.CLDR_RESULT+" where "+
                                                 Vetting.CLDR_RESULT+".base_xpath=CLDR_VET.base_xpath and "+
                                                 Vetting.CLDR_RESULT+".locale="+Vetting.CLDR_VET+".locale AND "+Vetting.CLDR_RESULT+".type="+
                                                 Vetting.RES_ADMIN+") AND EXISTS (select * from "+CLDR_DATA+" where "+
                                                 CLDR_DATA+".locale="+
                                                 Vetting.CLDR_VET+".locale AND "+CLDR_DATA+".xpath="+
                                                 Vetting.CLDR_VET+".vote_xpath and "+CLDR_DATA+".value != '')");
            keyNoVotesSet = prepareStatement("keyUnconfirmedSet",
                                             "select distinct "+CLDR_DATA+".xpath from "+CLDR_DATA+","+Vetting.CLDR_RESULT+" where "+CLDR_DATA+".locale=? AND "+CLDR_DATA+".locale="+Vetting.CLDR_RESULT+".locale AND "+CLDR_DATA+".xpath="+Vetting.CLDR_RESULT+".base_xpath AND "+Vetting.CLDR_RESULT+".type="+Vetting.RES_NO_VOTES);
            querySource = prepareStatement("querySource",
                                           "SELECT id,rev FROM " + CLDR_SRC + " where locale=? AND tree=? AND inactive IS NULL");
            
            querySourceInfo = prepareStatement("querySourceInfo",
                                               "SELECT rev FROM " + CLDR_SRC + " where id=?");
            
            querySourceActives = prepareStatement("querySourceActives",
                                                  "SELECT id,locale,rev FROM " + CLDR_SRC + " where inactive IS NULL");
            
            insertSource = prepareStatement("insertSource",
                                            "INSERT INTO " + CLDR_SRC + " (locale,tree,rev,inactive) VALUES (?,?,?,null)");
                                            
            getSubmitterId = prepareStatement("getSubmitterId",
                                            "SELECT submitter from " + CLDR_DATA + " where locale=? AND xpath=? AND ( submitter is not null )"); // don't return anything if the submitter isn't set.
                                            
            removeItem = prepareStatement("removeItem",
                                        "DELETE FROM " + CLDR_DATA + " where locale=? AND xpath=?");
        }
        
    }
    
    /**
     * the prepared statements used by this CLDRDBSource
     */
    MyStatements stmts = null;
    
    /** 
     * Initialize with (a reference to the shared) Connection
     * set up statements, etc.
     *
     * Verify that the Source data is available.
     */
    
    static int nn=0;
    
    private void initConn(Connection newConn, CLDRFile.Factory theFactory) {
        if(TRACE_CONN && SurveyMain.isUnofficial && (nn++%100)==0) {
            try {
                throw new Throwable("initConn called here");
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("initConns: "+nn);
            }
        }
        if(newConn != conn) {
            conn = newConn;
            if(stmts == null) {
                synchronized(conn) {
                    stmts = new MyStatements(newConn);  // prepare the statements
                }
            }
        }
   	if(rawXmlFactory==null) {
        	rawXmlFactory = theFactory;
	}
        
    }

    
    /**
     * the hashtable of ("tree_locale") -> Integer(srcId)
     */
    Hashtable srcHash = new Hashtable();
    //    
    public static final boolean USE_XPATH_CACHE=false; 
    
    /**
     * given a tree and locale, return the source ID.
     * @param tree which tree. should be "main". TODO: support multiple trees
     * @param locale the locale to fetch.
     * @return the source id, or -1 if not found.
     */
    public int getSourceId(String tree, String locale) {
        String key = tree + "_" + locale;
        
        synchronized (srcHash) {
            // first, is it in the hashtable?
            Integer r = null;
            r = (Integer) srcHash.get(key);
            if(r != null) {
                return r.intValue(); // quick check
            }
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
    
    
    /**
     * given a source ID, return the CVS revision # from the DB
     */
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
    
    /**
     * We are adding a new source ID to the database.  Add it, and get the #
     * @param tree which tree
     * @param locale which locale
     * @param rev CVS revision
     * @return the new source ID
     */
    private int setSourceId(String tree, String locale, String rev) {
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
                        throw new InternalError("CLDRDBSource: SQL failed to set source ("+tree + "/" + locale +")");
//                        return -1;
                    }
                } catch(SQLException se) {
                    throw new InternalError("CLDRDBSource: Failed to set source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
//                    return -1;
                }
            }
        }
    }

    /** 
     * Utility function called by manageSourceUpdates()
     * @see manageSourceUpdates
     */
    private void manageSourceUpdates_locale(WebContext ctx, SurveyMain sm, int id, CLDRLocale loc, boolean quietUpdateAll)
        throws SQLException
    {
        String mySql = ("DELETE from "+CLDR_DATA+" where source="+id+" AND submitter IS NULL");
//        logger.severe("srcupdate: "+loc+" - "+ mySql);
        Statement s = conn.createStatement();
        int r = s.executeUpdate(mySql);
        //ctx.println("<br>Deleting data from src " + id + " ... " + r + " rows.<br />");
        mySql = "UPDATE "+CLDR_SRC+" set inactive=1 WHERE id="+id;
        //logger.severe("srcupdate:  "+loc+" - " + mySql);
        int j = s.executeUpdate(mySql);
        //ctx.println(" Deactivating src: " + j + " rows<br />");
        ctx.println("Deactivated Source #"+id+"  and "+r+" rows of data<br>");
        logger.severe("srcupdate: " +loc + " - deleted " + r + " rows of data, and deactivated " + j + " rows of src ( id " + id +"). committing.. ");
        s.close();
        conn.commit();
        sm.lcr.invalidateLocale(loc); // force a reload.
    }
    
    public int manageSourceUpdates(WebContext ctx, SurveyMain sm) {
        return manageSourceUpdates(ctx, sm, false);
    }
    
    /**
     * Administrative hook called by the Admin user from SurveyMain
     * presents the "manage source updates" interface, allowing new XML files to be updated
     * @param ctx the webcontext
     * @param sm alias to the SurveyMain
     * @param quietUpdateAll if true, quietly do 'update all'
     */
    public int manageSourceUpdates(WebContext ctx, SurveyMain sm, boolean quietUpdateAll) {
        String what = ctx.field("src_update");
        boolean updAll = quietUpdateAll || what.equals("all_locs");
        int updated = 0;
        if(!quietUpdateAll) {
            ctx.println("<h4>Source Update Manager</h4>");
        }
        synchronized (sm.vet.conn) {
           // synchronized(xpt) {
                try {
                    boolean hadDiffs = false; // were there any differences? (used for 'update all')
                    ResultSet rs = stmts.querySourceActives.executeQuery();
                    if(!quietUpdateAll) {
                        ctx.println("<table border='1'>");
                        ctx.println("<tr><th>#</th><th>loc</th><th>DB Version</th><th>CVS/Disk</th><th>update</th></tr>");
                    }
                    while(rs.next()) {
                        int id = rs.getInt(1);
                        CLDRLocale loc = CLDRLocale.getInstance(rs.getString(2));
                        String rev = rs.getString(3);
                        String disk = LDMLUtilities.loadFileRevision(dir, loc+".xml");
                        if(!quietUpdateAll) {
                            ctx.println("<tr><th><a name='"+id+"'><tt>"+id+"</tt></a></th><td>" +loc + "</td>");
                            ctx.println("<td>db="+rev+"</td>");
                        }
                        if(rev == null )  {
                            rev = "null";
                        }
                        if(disk == null) {
                            disk = "null";
                        }
                        if(rev.equals(disk)) {
                            if(!quietUpdateAll) {
                                ctx.println("<td class='proposed'>-</td><td></td>"); // no update available
                            }
                        } else {
                            WebContext subCtx = new WebContext(ctx);
                            hadDiffs = true;
                            if(!quietUpdateAll) {
                                ctx.println("<td class='missing'>disk="+disk+ " </td> ");
                            }
                            subCtx.addQuery("src_update",loc.toString());
                            // ...
                            if(updAll || what.equals(loc)) { // did we request update of this one?
                                if(!quietUpdateAll) {
                                    ctx.println("<td class='proposed'>Updating...</td></tr><tr><td colspan='5'>");
                                } else {
                                    ctx.println("<br><b>"+loc + "</b> " + rev + " &mdash;&gt; "+disk + " ");
                                }
                                manageSourceUpdates_locale(ctx,sm,id,loc, quietUpdateAll);
                                updated++;
                                if(!quietUpdateAll) {
                                    ctx.println("</td>");
                                }
                            } else if(!quietUpdateAll) {
                                ctx.println("<td><a href='"+subCtx.url()+"#"+id+"'>Update</a></td>"); // update available
                            }
                        }
                        if(!quietUpdateAll) {
                            ctx.println("</tr>");
                        }
                    }
                    if(!quietUpdateAll) {
                        ctx.println("</table>");
                        if(hadDiffs) {
                                WebContext subCtx = new WebContext(ctx);
                                subCtx.addQuery("src_update","all_locs");
                                ctx.println("<p><b><a href='"+subCtx.url()+"'>Update ALL</b></p>");
                        }
                    }
                } catch(SQLException se) {
                    String complaint = ("CLDRDBSource: err in manageSourceUpdates["+what+"] ("+tree + "/" + "*" +"): " + SurveyMain.unchainSqlException(se));
                    logger.severe(complaint);
                    ctx.println("<hr /><pre>" + complaint + "</pre><br />");
                    return updated;
                }
          //  }
        }
        return updated;
    }

    /**
     * This is also called from the administrative pane and implements an old database migration function (adding base_xpaths).
     */
    public void doDbUpdate(WebContext ctx, SurveyMain sm) {
    
        if(true==true) {
            throw new InternalError("CLDRDBSource.doDbUpdate (to add base_xpaths) is obsolete and has been disabled."); // ---- obsolete.  Code left here for future use.
        }
         /* //       querySourceActives = prepareStatement("querySourceActives",
         //           "SELECT id,locale,rev FROM " + CLDR_SRC + " where inactive IS NULL");

         String what = ctx.field("db_update");
         //   boolean updAll = what.equals("all_locs");
         ctx.println("<h4>DB Update Manager (srl use only)</h4>");
         System.err.println("doDbUpdate: "+SurveyMain.freeMem());
         int n = 0, nd = 0;
         String loc = "_";
         synchronized (conn) {
            synchronized(xpt) {
                String sql="??";
                try {
                    boolean hadDiffs = false;
                    sql = "select xpath,base_xpath from CLDR_DATA WHERE ((BASE_XPATH IS NULL) OR (BASE_XPATH = -1)) AND locale=? FOR UPDATE";
                    PreparedStatement ps = conn.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE);
                    long t0 = System.currentTimeMillis();
                    for(Iterator iter = getAvailableLocales().iterator();iter.hasNext();) {
                        int thisn = 0;
                        loc = (String)iter.next();
                        ps.setString(1,loc);
                        ResultSet rs = ps.executeQuery();
                        while(rs.next()) {
                            int oldXpath = rs.getInt(1);
                            int newXpath = xpt.xpathToBaseXpathId(oldXpath);
                            n++;
                            thisn++;
                            if(newXpath!=oldXpath) {
                                nd++;
                            }
                            rs.updateInt(2,newXpath);
                            rs.updateRow();
                            if((n%1000)==0) {
                                ctx.println(loc + " - " + n + " updated, " + nd + " had differences<br>");
                                long td = System.currentTimeMillis()-t0;
                                double per = ((double)n/(double)td)*1000.0;
                                System.err.println("CLDBSource.doDbUpdate: "  + n + "update, @"+loc + ", " + nd + " had difference.  Avg " + per + "/sec. "+SurveyMain.freeMem());
                            }
                        }
                        if(thisn>0) {
                            System.err.println(loc + " Committing " + thisn + " items ... "+SurveyMain.freeMem());
                            conn.commit();
                        }
                    }
                    ctx.println("DONE: " + n + "patched, " + nd + " had difference.<br>");
                    System.err.println("CLDBSource.doDbUpdate:  DONE, " + n + "patched, " + nd + " had difference. "+SurveyMain.freeMem());
                    
                } catch(SQLException se) {
                    String complaint = ("CLDRDBSource: err in doDbUpdate["+what+"] ("+tree + "/" + "*" +"): " + SurveyMain.unchainSqlException(se) + " - loc was = " + loc + " and SQL was: " + sql);
                    logger.severe(complaint);
                    ctx.println("<hr /><pre>" + complaint + "</pre><br />");
                    return;
                }
            }
         }
          */
    }
    


    public int getSubmitterId(CLDRLocale locale, int xpath) {
    	synchronized(sm.vet.conn) {
	      try {
	          ResultSet rs;
	          stmts.getSubmitterId.setString(1,locale.toString());
	          stmts.getSubmitterId.setInt(2,xpath);
	          rs = stmts.getSubmitterId.executeQuery();
	          if(!rs.next()) {
	  ///*srl*/     System.err.println("GSI[-1]: " + locale+":"+xpath);
	              return -1;
	          }
	          int rp = rs.getInt(1);
	          rs.close();
	  ///*srl*/ System.err.println("GSI["+rp+"]: " + locale+":"+xpath);
	          if(rp > 0) {
	              return rp;
	          } else {
	              return -1;
	          }
	      } catch(SQLException se) {
	          logger.severe("CLDRDBSource: Failed to getSubmitterId ("+tree + "/" + locale + ":" + xpt.getById(xpath) + "#"+xpath+"): " + SurveyMain.unchainSqlException(se));
	          throw new InternalError("Failed to getSubmitterId ("+tree + "/" + locale + ":" + xpt.getById(xpath) + "#"+xpath + "): "+se.toString()+"//"+SurveyMain.unchainSqlException(se));
	      }
    	}
    }



  ///*srl*/        boolean showDebug = (path.indexOf("dak")!=-1);
  //if(showDebug) /*srl*/logger.info(locale + ":" + path);
    public int getWinningPathId(int xpath, CLDRLocale locale, boolean finalData) {
        if(finalData) {
            return sm.xpt.xpathToBaseXpathId(xpath);
        }
        
      if(false) {
          try {
              ResultSet rs;
              if(finalData) {
                  return sm.xpt.xpathToBaseXpathId(xpath);
                  //throw new InternalError("Unsupported: getWinningPath("+xpath+","+locale+") on finalData");
              } else {
                  stmts.queryVetXpath.setString(1,locale.toString());
                  stmts.queryVetXpath.setInt(2,xpath); 
                  rs = stmts.queryVetXpath.executeQuery();
              }
              if(!rs.next()) {
                  return -1;
              }
              int rp = rs.getInt(1);
              rs.close();
              if(rp != 0) {  // 0  means, fallback xpath
                  return rp;
              } else {
                  return -1;
              }
              //if(showDebug)/*srl*/if(finalData) {    logger.info(locale + ":" + path+" -> " + rv);}
          } catch(SQLException se) {
              se.printStackTrace();
              logger.severe("CLDRDBSource: Failed to getWinningPath ("+tree + "/" + locale + ":" + xpt.getById(xpath) + "#"+xpath+"): " + SurveyMain.unchainSqlException(se));
              throw new InternalError("Failed to getWinningPath ("+tree + "/" + locale + ":" + xpt.getById(xpath) + "#"+xpath + "): "+se.toString()+"//"+SurveyMain.unchainSqlException(se));
          }
      } else {
          return sm.vet.getWinningXPath(xpath, locale);
      }
    }

    public String getWinningPath(int xpath, CLDRLocale locale, boolean finalData) {
      int rp = getWinningPathId(xpath, locale, finalData);
      if(rp > 0) {
          return xpt.getById(rp);
      } else {
          return null;
      }
    }
    
    private static int makeHashHitCount = 0;
    private static int maxMakeHashSize = 0;
    static final boolean MAKE_CACHE = false;

    /**
     * Close out the connection.
     */
    public void closeConnection() {
        SurveyMain.closeDBConnection(conn);
        conn = null;
    }
    
    /**
     * return a list fo XML input files
     */
    private File[] getInFiles() {
        // 1. get the list of input XML files
        FileFilter myFilter = new FileFilter() {
            public boolean accept(File f) {
                String n = f.getName();
                return(!f.isDirectory()
                       &&n.endsWith(".xml")
                       &&!n.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX) // not a locale
                       /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
            }
        };
        File baseDir = new File(dir);
        return baseDir.listFiles(myFilter);
    }


    /*
     * 
========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= 
========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= 
========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= ========= 
     */
    /**
 * This class implements an XMLSource which is read out of a database.
 * it is not directly modifiable, but it does have routines for modifying the database again.
 **/
 public class CLDRDBSource extends XMLSource {
     
////    
////     void setup() {
////         if(!loadAndValidate(getLocaleID(), null)) {
////             throw new InternalError("Couldn't load and validate: " + getLocaleID());
////         }
//
//     }
    /**
     * show final, vetted data only?
     */
    public boolean finalData = false;
    
    /**
     * for vetting? 
     */
    //private Vetting vetting = null;
    
    
    // local things
    private Comments xpath_comments = new Comments(); 

    /**
     * User this File belongs to (or null)
     * @deprecated
     */
    protected UserRegistry.User user= null; 
    

    /** 
     * source ID of this CLDRDBSource. 
     * @see #getSourceId
     */
    public int srcId = -1; 

    /**
     * load and validate the item, if not already in the DB. Sets srcId and other state.
     * Note that this is not a fully resolved operation at this level.
     * @param locale the locale to load. Ex:  "mt_MT"
     **/
    boolean setLocaleAndValidate(String locale) {
        setLocaleID(locale);
        srcId = getSourceId(tree, locale);

        if(srcId != -1) { 
            return true;  // common case. The locale is already loaded. We're done.
        }

        System.err.println("@sl&v: "+locale+"/"+srcId);

        synchronized(conn) {  // Synchronize on the conn to ensure that no other state is changing under us..
            // double check..
            srcId = getSourceId(tree, locale); // double checked lock- noone else loaded the src since then
            
            if(srcId != -1) { 
                return true;  // common case.
            }            
            if(conn == null) {
                throw new InternalError("loadAndValidate: failed, no DB connection"); // very bad, our DB connection went away.
            }
            
            String rev = LDMLUtilities.loadFileRevision(dir, locale+".xml");  // Load the CVS version # as a string
            if(rev == null) rev = "null";
            srcId = setSourceId(tree, locale, rev); // TODO: we had better fill it in..
        //    synchronized(conn) {            
               // logger.info("srcid: " + srcId +"/"+locale);
               // if(locale.equals("el__POLYTON")) locale="el_POLYTON";
                CLDRFile file = rawXmlFactory.make(locale, false, true); // create the CLDRFile pointing to the raw XML
                
                if(file == null) {
                    System.err.println("Couldn't load CLDRFile for " + locale);
                    return false ;
                }
                
                System.err.println("loading rev: " + rev + " for " + dir + ":" + srcId +"/"+locale+".xml"); // LOG that a new item is loaded.
                int rowCount=0;
                //sm.fora.updateBaseLoc(locale); // in case this is a new locale.
                // Now, start loading stuff in
                XPathParts xpp=new XPathParts(null,null); // for parsing various xpaths
                
                for (Iterator it = file.iterator(); it.hasNext();) {  // loop over the contents of the raw XML ..
                    rowCount++;
                    String rawXpath = (String) it.next();
                    
                    // Make it distinguished
                    String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);
                    
                    //if(!xpath.equals(rawXpath)) {
                    //    logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
                    //}
                    
                    String oxpath = file.getFullXPath(xpath); // orig-xpath.  
                    
                    if(!oxpath.equals(file.getFullXPath(rawXpath))) {
                        // Failed the sanity check.  This should Never Happen(TM)
                        // What's happened here, is that the full xpath given the raw xpath, ought to be the full xpath given the distinguished xpath.
                        // SurveyTool depends on this being reversable thus.
                        throw new InternalError("FATAL: oxpath and file.getFullXPath(raw) are different: " + oxpath + " VS. " + file.getFullXPath(rawXpath));
                    }
                    
                    int xpid = xpt.getByXpath(xpath);       // the numeric ID of the xpath
                    int oxpid = xpt.getByXpath(oxpath);     // the numeric ID of the orig-xpath
                    
                    String value = file.getStringValue(xpath); // data value from XML
                    
                    // Now, munge the xpaths around a bit.
                    xpp.clear();
                    xpp.initialize(oxpath);
                    String lelement = xpp.getElement(-1);
                    /* all of these are always at the end */
                    String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
                    String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
        
                    /* we call a special function to find the "tiny" xpath.  Which see */
                    String eType = xpt.typeFromPathToTinyXpath(xpath, xpp);  // etype = the element's type
                    String tinyXpath = xpp.toString(); // the tiny xpath
                    
                    int txpid = xpt.getByXpath(tinyXpath); // the numeric ID of the tiny xpath
                    
                    int base_xpid = xpt.xpathToBaseXpathId(xpath);  // the BASE xpath 
                    
                    /* Some debugging to print these various things*/ 
    //                System.out.println(xpath + " l: " + locale);
    //                System.out.println(" <- " + oxpath);
    //                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
    //                System.out.println(" => "+txpid+"#" + tinyXpath);
                      
                    // insert it into the DB
                    try {
                        stmts.insert.setInt(1,xpid); // full
                        stmts.insert.setString(2,locale);
                        stmts.insert.setInt(3,srcId);
                        stmts.insert.setInt(4,oxpid); // Note: assumes XPIX = orig XPID! TODO: fix
                        SurveyMain.setStringUTF8(stmts.insert, 5, value); // stmts.insert.setString(5,value);
                        stmts.insert.setString(6,eType);
                        stmts.insert.setString(7,eAlt);
                        stmts.insert.setInt(8,txpid); // tiny
                        stmts.insert.setNull(9, java.sql.Types.INTEGER); // Null integer for Submitter. NB: we do NOT ever consider data coming from XML as 'submitter' data.
                        stmts.insert.setInt(10, base_xpid);

                        stmts.insert.execute();
                        
                    } catch(SQLException se) {
                        String complaint = 
                            "CLDRDBSource: Couldn't insert " + locale + ":" + xpid + "(" + xpath +
                                ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
                        logger.severe(complaint);
                        throw new InternalError(complaint);
                    }
                }
                
                try{
                        conn.commit();
                } catch(SQLException se) {
                        String complaint = 
                            "CLDRDBSource: Couldn't commit " + locale +
                                ":" + SurveyMain.unchainSqlException(se);
                        logger.severe(complaint);
                        throw new InternalError(complaint);
                }
                System.err.println("loaded " + rowCount + " rows into  rev: " + rev + " for " + dir + ":" + srcId +"/"+locale+".xml"); // LOG that a new item is loaded.
                CLDRLocale loc = CLDRLocale.getInstance(locale);
              	needUpdate(loc);
                return true;
         //   }
        } // end: synch(xpt)
    }
        

    /**
     * Return the SCM ID of this source.
     */
    public String getSourceRevision() {
        return CLDRDBSourceFactory.this.getSourceRevision(srcId); // TODO: awkward
    }
    

    
    /**
     * Return the path to supplemental data.
     */
    public File getSupplementalDirectory() {
        File suppDir =  new File(dir+"/../"+"supplemental");
        return suppDir;
    }
    
	
    public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) { 
        complain_about_slower_api();
        putValueAtPath(fullxpath, this.getValueAtDPath(distinguishingXPath));
    }
    
    public void putValueAtDPath(String distinguishingXPath, String value) { 
        complain_about_slower_api();
        putValueAtPath(this.getFullPathAtDPath(distinguishingXPath), value);
    }
    public String putValueAtPath(String xpath, String value)
    {
        String loc = getLocaleID();
        String dpath = CLDRFile.getDistinguishingXPath(xpath, null, false);
        CLDRLocale locale = CLDRLocale.getInstance(loc);
        int xpid = sm.xpt.getByXpath(dpath);       // the numeric ID of the xpath
        int oxpid = sm.xpt.getByXpath(xpath);     // the numeric ID of the orig-xpath
        // Make it distinguished
        //if(!xpath.equals(rawXpath)) {
        //    logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
        //}
        
        //String oxpath = file.getFullXPath(xpath); // orig-xpath.  
        
//        if(!oxpath.equals(file.getFullXPath(rawXpath))) {
//            // Failed the sanity check.  This should Never Happen(TM)
//            // What's happened here, is that the full xpath given the raw xpath, ought to be the full xpath given the distinguished xpath.
//            // SurveyTool depends on this being reversable thus.
//            throw new InternalError("FATAL: oxpath and file.getFullXPath(raw) are different: " + oxpath + " VS. " + file.getFullXPath(rawXpath));
//        }
//        
        
        XPathParts xpp = new XPathParts(null,null);
        // Now, munge the xpaths around a bit.
        xpp.clear();
        xpp.initialize(xpath);
        String lelement = xpp.getElement(-1);
        /* all of these are always at the end */
        String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
        int submitter = XPathTable.altProposedToUserid(eAlt);
        String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);

        /* we call a special function to find the "tiny" xpath.  Which see */
        String eType = sm.xpt.typeFromPathToTinyXpath(xpath, xpp);  // etype = the element's type
        String tinyXpath = xpp.toString(); // the tiny xpath
        
        int txpid = sm.xpt.getByXpath(tinyXpath); // the numeric ID of the tiny xpath
        
        int base_xpid = sm.xpt.xpathToBaseXpathId(dpath);  // the BASE xpath 
        
        /* Some debugging to print these various things*/ 
//                System.out.println(xpath + " l: " + locale);
//                System.out.println(" <- " + oxpath);
//                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
//                System.out.println(" => "+txpid+"#" + tinyXpath);
          
        // insert it into the DB
    	synchronized(conn) {
        try {
            stmts.insert.setInt(1,xpid);  /// dpath
            stmts.insert.setString(2,loc);
            stmts.insert.setInt(3,srcId);
            stmts.insert.setInt(4,oxpid);  // origxpath = full (original) xpath
            SurveyMain.setStringUTF8(stmts.insert, 5, value); // stmts.insert.setString(5,value);
            stmts.insert.setString(6,eType);
            stmts.insert.setString(7,eAlt);
            stmts.insert.setInt(8,txpid); // tiny xpath
            if(submitter == -1) {
                    stmts.insert.setNull(9, java.sql.Types.INTEGER); //  getId(alt...)
            } else {
                stmts.insert.setInt(9, submitter);
            }
            stmts.insert.setInt(10, base_xpid);

            stmts.insert.execute();
            
            System.err.println("Inserted: " + xpath);
            
        } catch(SQLException se) {
            String complaint = 
                "CLDRDBSource: Couldn't insert " + getLocaleID() + ":" + xpid + "(" + xpath +
                    ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
            logger.severe(complaint);
            throw new InternalError(complaint);
        }
    
    try{
            conn.commit();
    } catch(SQLException se) {
            String complaint = 
                "CLDRDBSource: Couldn't commit " + getLocaleID() +
                    ":" + SurveyMain.unchainSqlException(se);
            logger.severe(complaint);
            throw new InternalError(complaint);
    }
//    System.err.println("loaded " + rowCount + " rows into  rev: " + rev + " for " + dir + ":" + srcId +"/"+locale+".xml"); // LOG that a new item is loaded.
//    if(vetterReady) {
//        synchronized(sm.vet) {
//            sm.vet.updateResults(loc);
//        }
//    } else {
//        System.err.println("CLDRDBSource " + loc + " - deferring vet update on " + loc + " until vetter ready.");
//    }
    	} /* release the outer conn connection */
        needUpdate(locale);
        return dpath;
    }

    /**
     * XMLSource API. Unimplemented, read only.
     */
    public void removeValueAtDPath(String distinguishingXPath) {  
        //String dpath = CLDRFile.getDistinguishingXPath(xpath, null, false);
        String loc = getLocaleID();
        CLDRLocale locale = CLDRLocale.getInstance(loc);
        int xpid = sm.xpt.getByXpath(distinguishingXPath);       // the numeric ID of the xpath
        removeItem(locale, xpid);
    }


    /**
     * Remove an item from the DB. 
     * @param locale locale of item
     * @param xpathId id to remove
     * @return number of rows deleted
     */
    public int removeItem(CLDRLocale locale, int xpathId) {
        if(conn == null) {
            throw new InternalError("No DB connection!");
        }
        try {
            stmts.removeItem.setString(1, locale.toString());
            stmts.removeItem.setInt(2, xpathId); // base xpath
 //           stmts.removeItem.setInt(3, submitter);
            int n = stmts.removeItem.executeUpdate();
            if(n != 1) {
                throw new InternalError("Trying to remove "+locale+":"+xpathId+"@" + " and the path wasn't found.");
            }
            conn.commit();
            return n;
        } catch(SQLException se) {
            String problem = ("CLDRDBSource: "+"Trying to remove "+locale+":"+xpathId+"@" +" : " + SurveyMain.unchainSqlException(se));
            logger.severe(problem);
            throw new InternalError(problem);
        }
    }

   /** 
     * XMLSource API. Returns whether or not a value exists. 
     * @param path a distinguished path
     * @return true if the value exists
     */
    public boolean hasValueAtDPath(String path) // d path
    {
        long t0;
        String locale = getLocaleID();
        if(SHOW_TIMES) {
               t0 = System.currentTimeMillis();
        }
        if(finalData) {
            boolean rv =  (getValueAtDPath(path) != null); // TODO: optimize this
            if(SHOW_TIMES) System.err.println("hasValueAtDPath:final "+locale + ":" + path + " " + (System.currentTimeMillis()-t0));
            return rv;
        }
    
        if(conn == null) {
            throw new InternalError("No DB connection!");
        }
        
        int pathInt = xpt.getByXpath(path);
        if(SHOW_TIMES) System.err.println("hasValueAtDPath:>> "+locale + ":" + pathInt + " " + (System.currentTimeMillis()-t0));
    
        //logger.info(locale + ":" + path);
        //synchronized (conn) {
            try {
                stmts.queryValue.setString(1,locale);
                stmts.queryValue.setInt(2,pathInt); 
                ResultSet rs = stmts.queryValue.executeQuery();
                if(rs.next()) {
                    if(SHOW_TIMES) System.err.println("hasValueAtDPath:T "+locale + ":" + path + " " + (System.currentTimeMillis()-t0));
                    return true;
                } else {
                    if(SHOW_TIMES) System.err.println("hasValueAtDPath:F "+locale + ":" + path + " " + (System.currentTimeMillis()-t0));
                    return false;
                }
                //                rs.close();
                //logger.info(locale + ":" + path+" -> " + rv);
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to check data ("+tree + "/" + locale + ":" + path + "): " + SurveyMain.unchainSqlException(se));
                return false;
            }
        //}
    }



  public String getWinningPath(String path) 
  {
    int xpath=xpt.xpathToBaseXpathId(path);
    
    // look for it in parents
    for(CLDRLocale locale : CLDRLocale.getInstance(getLocaleID()).getParentIterator()) {
        String rv = CLDRDBSourceFactory.this.getWinningPath(xpath, locale, finalData);
        
        
        if(false&&path.indexOf("=\"TR")>0&&path.indexOf("symbol")>0) {
        	System.err.println("GWP: "+locale+",fd="+finalData+", "+xpath+":"+path+" -> "+rv);
        }
        
        
        if(rv != null) {
            return rv;
        }
    }
    return xpt.getById(xpath); // default: winner is original path
//    throw new InternalError("Can't find winning path for getWinningPath("+path + "#"+xpath+","+getLocaleID()+")");
  }
    
    
    /**
     * XMLSource API. Returns the value of a distringuished path
     * @param path distinguished path
     * @return value or else null if none exists
     */
     
    public String getValueAtDPath(String path) // D path
    {
    	synchronized(this.conn) {
	        long t0;
	        if(SHOW_TIMES) t0 = System.currentTimeMillis();
	        if(conn == null) {
	            throw new InternalError("No DB connection!");
	        }
	    
	        String locale = getLocaleID();
	        int xpath = xpt.getByXpath(path);
	        if(SHOW_TIMES) System.err.println("hasValueAtDPath:>> "+locale + ":" + xpath + " " + (System.currentTimeMillis()-t0));
	
	        ///*srl*/        boolean showDebug = (path.indexOf("dak")!=-1);
	//        try {
	//            throw new InternalError("bar");
	//        } catch(InternalError ie) {
	//            ie.printStackTrace();
	//        }
	        if(SHOW_DEBUG) /*srl*/logger.info(locale + ":" + path);
	        try {
	            ResultSet rs;
	            if(finalData) {
	                stmts.queryVetValue.setString(1,locale);
	                stmts.queryVetValue.setInt(2,xpath); 
	                rs = stmts.queryVetValue.executeQuery();
	            } else {
	                stmts.queryValue.setString(1,locale);
	                stmts.queryValue.setInt(2,xpath);
	                rs = stmts.queryValue.executeQuery();
	            }
	            String rv;
	            if(!rs.next()) {
	                if(SHOW_TIMES) System.err.println("hasValueAtDPath:0 "+locale + ":" + path + " " + (System.currentTimeMillis()-t0));
	                if(!finalData) {
	                    rs.close();                    
	                    if(SHOW_DEBUG) System.err.println("Nonfinal - no match for "+locale+":"+xpath + "");
	                    return null;
	                } else {
	                    if(SHOW_DEBUG) System.err.println("Couldn't find "+ locale+":"+xpath + " - trying original - @ " + path);
	                    //if(locale.equals("de")) {
	                    //    return "fork";
	                    //} else {
	
	                    if(false && sm.isUnofficial) {
	                        if(oldKeySet().contains(path)) {
	                            System.err.println("Ad but missing: "+ locale+":"+xpath + " - @ " + path);
	                        } else {
	                            if(SHOW_DEBUG) System.err.println("notad: "+ locale+":"+xpath + " - @ " + path);
	                        }
	                    }
	                    rs.close();
	                    return null;
	                }                      
	            }
	            rv = SurveyMain.getStringUTF8(rs, 1); //            rv = rs.getString(1); // unicode
	            if(SHOW_TIMES) System.err.println("hasValueAtDPath:+ "+locale + ":" + path + " " + (System.currentTimeMillis()-t0));
	            if(rs.next()) {
	                String complaint = "warning: multi return: " + locale + ":" + path + " #"+xpath;
	                logger.severe(complaint);
	               // throw new InternalError(complaint);                    
	            }
	            rs.close();
	            if(SHOW_DEBUG) if(finalData) {    logger.info(locale + ":" + path+" -> " + rv);}
	            return rv;
	        } catch(SQLException se) {
	            se.printStackTrace();
	            logger.severe("CLDRDBSource: Failed to query data ("+tree + "/" + locale + ":" + path + "): " + SurveyMain.unchainSqlException(se));
	            return null;
	        }
    	}
    }

    /*
     * convert a distinguished path to a full path
     * @param path cleaned (distinguished) path
     * @return the full path
     */
    public String getFullPathAtDPath(String path) {
        String ret =  getOrigXpathFromCache(xpt.getByXpath(path));
        if(ret == null ) {
        	return path;
        	/*
//            // Debug / validation
            String try2 = getOrigXpathString(xpt.getByXpath(path),finalData);
            if(try2 != null) {
                System.err.println("Cache failed: was null at " +xpt.getByXpath(path)+":"+ path);
                ret = try2;
            } else {
            	System.err.println("Null gfx:"+xpt.getByXpath(path));
            }*/
        }
        return ret;
    }
    
    public void setLocaleID(String localeID) {
        super.setLocaleID(localeID);
        reset();
    }
    
    /**
     * Reset per-locale cache, re-register.
     */
    private void reset() {
        origXpaths.clear();
        token = new Registerable(sm.lcr, CLDRLocale.getInstance(getLocaleID()));
        token.register();
    }
    
    Registerable token = null;

    /**
     * get the 'original' xpath from a path-id#
     * @param pathid ID# of a path
     * @return the original xpath string
     * @see XPathTable
     */
    public String getOrigXpathFromCache(int pathid) {
        if(!USE_XPATH_CACHE) {
            return getOrigXpath(pathid, finalData);
        } else {
            if(token==null||!token.isValid()) {
                reset();
            }
            Integer orig = origXpaths.get(pathid);
            if(orig == null) {
                orig = getOrigXpathId(pathid, finalData);
                origXpaths.put(pathid, orig);
                System.err.println("Rescan: locale,"+pathid+","+orig);
            }
            if(orig==null || orig==-1) {
                return null;
            } else {
                return sm.xpt.getById(orig);
            }
        }
    }

    IntHash<Integer> origXpaths = new IntHash<Integer>();

    /**
     * get the 'original' xpath from a path-id#
     * @param pathid ID# of a path
     * @return the original xpath string
     * @see XPathTable
     */
    public String getOrigXpath(int pathid, boolean useFinalData) {
        return getOrigXpathString(pathid, useFinalData);
    }
    

    private final String getOrigXpathString(int pathid, boolean useFinalData) {
        int n = getOrigXpathId(pathid, useFinalData);
        return sm.xpt.getById(n);
    }
    
    public int getOrigXpathId(int pathid, boolean useFinalData) {
        String locale = getLocaleID();
        synchronized (conn) { // NB: many of these synchronizeds were removed as unnecessary.
            try {
				ResultSet rs;
				
				if(!useFinalData) {
					stmts.oxpathFromXpath.setInt(1,pathid);
					stmts.oxpathFromXpath.setString(2,locale);
					rs = stmts.oxpathFromXpath.executeQuery();
				} else {
					stmts.oxpathFromVetXpath.setString(1, locale);
					stmts.oxpathFromVetXpath.setInt(2, pathid);
					rs = stmts.oxpathFromVetXpath.executeQuery();
				}
				
                if(!rs.next()) {
                    rs.close();
                    //System.err.println("getOrigXpath["+finalData+"/"+useFinalData+"] not found, falling back: " + locale + ":"+pathid+" " + xpt.getById(pathid));
                    return -1;
//                    throw new InternalError  /* logger.severe */ ("getOrigXpath["+finalData+"] not found, falling back: " + locale + ":"+pathid+" " + xpt.getById(pathid));
                    //return xpt.getById(pathid); // not found - should be null?
                
                }
                int result = rs.getInt(1);
				/*
                if(rs.next()) {
                    logger.severe("getOrigXpath returns two results: " + locale + " " + xpt.getById(pathid));
                    // fail?? what?
                }*/
                rs.close();
                return result;
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to find orig xpath ("+tree + "/" + locale +"/"+xpt.getById(pathid)+"): " + SurveyMain.unchainSqlException(se));
                return pathid; //? should be null?
            }
        }
    }
    
    /**
     * return the comments array, which comes from the raw XML.
     * @return comments
     * @see Comments
     */
    public Comments getXpathComments() {
        CLDRFile file = rawXmlFactory.make(getLocaleID(), false, true);
        return file.getXpath_comments();
    }

    /**
     * set the comments array, which comes from the raw XML.
     * @see Comments
     */
	public void setXpathComments(Comments path) {
        this.xpath_comments = path;
    }
 
    /** 
     * TODO: This could take a while (given vetting rules)- do we need it?
     */
    public int size() {
        throw new InternalError("not implemented yet");
    }
    
    /**
     * Get a list of which locales are available to this source, as per the underlying data store.
     * @return set of available locales
     */
    public Set getAvailableLocales() {
        // TODO: optimize
        File inFiles[] = getInFiles();
        Set<String> s = new HashSet<String>();
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
                s.add(localeName);
            }
        }
        return s;
    }
    
    /**
     * Cache of iterators over various things
     */
    Hashtable keySets = new Hashtable();
    
    /**
     * Return an iterator for the current set.
     */
    public Iterator iterator() {
        if(finalData) { // don't cache finaldata iterator.
            return oldKeySet().iterator();
        }
        
        String k = getLocaleID();
        Set s = (Set)keySets.get(k);
        if(s==null) {
            System.err.println("CCLCDRDBSource iterator: " + k);
            s = oldKeySet();
            keySets.put(k,s);
        }
        return s.iterator();
    }

    /** 
     * Return an iterator for a specific xpath prefix. This is faster than iterating over all
     * functions, and discarding the ones the caller doesn't want. 
     * @param prefix prefix of xpaths 
     * @return an iterator over the specified paths.
     */
    public Iterator iterator(String prefix) {
        if(finalData) {
            return super.iterator(prefix); // no optimization for this, yet
        } else {
            com.ibm.icu.dev.test.util.ElapsedTimer et;
            if(SHOW_TIMES) et= new com.ibm.icu.dev.test.util.ElapsedTimer();
            Iterator i =  prefixKeySet(prefix).iterator();
            if(SHOW_TIMES)   logger.info(et + " for iterator on " + getLocaleID() + " prefix " + prefix);
            return i;
        }
     }

    /**
     * @deprecated
     * Returns the old, slower format iterator
     * (this is the only way to get only-final data)
     * TODO: rewrite as iterator
     */
    private Set oldKeySet() {
        String locale = getLocaleID();
		try {
			ResultSet rs;
			if(finalData==false) {
				stmts.keySet.setString(1,locale);
				rs = stmts.keySet.executeQuery();
			} else {
	           //System.err.println("@@ begin KS of "+locale);
				stmts.keyVettingSet.setString(1,locale);
				rs = stmts.keyVettingSet.executeQuery();
			}
			// TODO: is there a better way to map a ResultSet into a Set?
			Set<String> s = new HashSet<String>();
			while(rs.next()) {
				int xpathid = rs.getInt(1);
          // if(finalData) { System.err.println("v|"+locale+":"+xpathid); }
				
				String xpath = (xpt.getById(xpathid));
				if(finalData==true) {
				
//                       System.err.println("Path: " +xpath);
					if(xpathThatNeedsOrig(xpath)) {
//                            System.err.println("@@ munging xpath:"+xpath+" ("+xpathid+")");
						xpath = getOrigXpathFromCache(xpathid);
//                            System.err.println("-> "+xpath);
					}
			
				
				}
				s.add(xpath); // xpath
				//rs.getString(2); // origXpath
			}
			rs.close();
           // if(finalData) System.err.println("@@ end KS of "+locale);
/*
			// keySet has  prov/unc already.
			if(finalData) {
				// also add provisional and unconfirmed items

				// provisional: "at least one Vetter has voted for it"
				// unconfirmed: "a Guest Vetter has voted for it"
				stmts.keyUnconfirmedSet.setString(1,locale);
				rs = stmts.keyUnconfirmedSet.executeQuery();
				while(rs.next()) {
					int xpathid = rs.getInt(1);
					String xpath = (xpt.getById(xpathid));
					s.add(xpath);
				}
				rs.close();
				
				// Now, add items that had no votes. 
				stmts.keyNoVotesSet.setString(1,locale);
				rs = stmts.keyNoVotesSet.executeQuery();
				while(rs.next()) {
					int xpathid = rs.getInt(1);
					String xpath = (xpt.getById(xpathid));
					s.add(xpath);
				}
				rs.close();
			}
*/
			return Collections.unmodifiableSet(s);
		} catch(SQLException se) {
			logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
			throw new InternalError("CLDRDBSource: Failed to query source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
		}
    }

    /**
     * is this an xpath which requires the 'origXpath' to make sense of it?
     * i.e. 'alias' contains attribute data 
     * TODO: discover this dynamically 
     */
    final private boolean xpathThatNeedsOrig(String xpath) {
     if(xpath.endsWith("/minDays") ||
        xpath.endsWith("/default") ||
        xpath.endsWith("/alias") ||
        xpath.endsWith("/orientation") ||
        xpath.endsWith("/weekendStart") ||
        xpath.endsWith("/weekendEnd") ||
        xpath.endsWith("/measurementSystem") ||
        xpath.endsWith("/singleCountries") ||
        xpath.endsWith("/abbreviationFallback") ||
        xpath.endsWith("/preferenceOrdering") ||
        xpath.endsWith("/inList") ||
        xpath.endsWith("/firstDay") ) {
        return true;
     }
     return false;
    }


    /**
     * return the keyset over a certain prefix
     */
    private Set prefixKeySet(String prefix) {
//        String locale = getLocaleID();
//        synchronized (conn) {
            try {
//                stmts.keySet.setString(1,locale);
                ResultSet rs = getPrefixKeySet(prefix);
                
                // TODO: is there a better way to map a ResultSet into a Set?
                Set<String> s = new HashSet<String>();
//                System.err.println("@tlh: " + "BEGIN");
                while(rs.next()) {
                    String xpath = (xpt.getById(rs.getInt(1)));
                    //if(-1!=xpath.indexOf("tlh")) {
                    //    xpath = xpath.replaceAll("\\[@draft=\"true\"\\]","");
//                        System.err.println("@tlh: " + xpath);
                    //}
                    s.add(xpath); // xpath
                    //rs.getString(2); // origXpath
                }
//                System.err.println("@tlh: " + "END");
                return Collections.unmodifiableSet(s);
                // TODO: 0
            } catch(SQLException se) {
                logger.severe("CLDRDBSource: Failed to query source ("+tree + "/" + getLocaleID() +"): " + SurveyMain.unchainSqlException(se));
                return null;
            }
//        }
    }

    
    /**
     * Table of all aliases
     */
    private Hashtable aliasTable = new Hashtable();
    
    /**
     * add all the current aliases to the parameter
     * @param output the list to be added to
     * @return a reference to output
     */
	public List addAliases(List output) {
        String locale = getLocaleID();
//        com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
        List aList = getAliases();
//        logger.info(et + " for getAlias on " + locale);
        output.addAll(aList);
        return output;
    }

    /**
     * get a copy of all aliases 
     * @return list of aliases
     */
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
                } catch(SQLException se) {
                    logger.severe("CLDRDBSource: Failed to query A source ("+tree + "/" + locale +"): " + SurveyMain.unchainSqlException(se));
                    return null;
                }
            }
            return output;
        }
    }
    
    Hashtable<String, XMLSource> makeHash = new Hashtable<String, XMLSource>();

    /**
     * @deprecated
     */
    public final XPathTable xpt = CLDRDBSourceFactory.this.xpt;

    /**
     * @deprecated
     */
    public final Connection conn = CLDRDBSourceFactory.this.conn;
    
    
    
    
    /**
     * Factory function. Create a new XMLSource from the specified id. 
     * clones this's db conn, etc.
     * @param localeID the id to make
     * @return the new CLDRDBSource
     */
    public XMLSource make(String localeID) {
        if(localeID == null) return null; // ???
        XMLSource result = null;
        if(MAKE_CACHE) result = makeHash.get(localeID);
        if(result == null) {
            if(localeID.startsWith(CLDRFile.SUPPLEMENTAL_PREFIX)) {
                XMLSource msource = new CLDRFile.SimpleXMLSource(rawXmlFactory, localeID).make(localeID);
                msource.freeze();
//                System.err.println("Getting simpleXMLSource for " + localeID);
                result = msource; 
            } else {
                CLDRDBSource dbresult = (CLDRDBSource)clone();
                if(!localeID.equals(dbresult.getLocaleID())) {
                    //dbresult.setLocaleID(localeID);
                    //dbresult.initConn(conn, rawXmlFactory); // set up connection & prepared statements. conn/factory may be set twice.
                    dbresult.setLocaleAndValidate(localeID);
                }
                result = dbresult;
            }
            if(MAKE_CACHE) makeHash.put(localeID, result);
        } else if(TRACE_CONN && SurveyMain.isUnofficial) {
            if(makeHash.size()>maxMakeHashSize) {
                maxMakeHashSize=makeHash.size();
            }
            makeHashHitCount++;
            if((makeHashHitCount%1000) == 0) {
                System.err.println("make: cache hit "+makeHashHitCount+" times, hash size " + makeHash.size() + " (max " + maxMakeHashSize+"), initConn count " + nn);
                if(false&& (makeHashHitCount % 1000)==0){
                    try {
                        throw new Throwable("cache hit make() called here");
                    } catch(Throwable t) {
                        t.printStackTrace();
                    }                
                }                    
            }
        }
        return result;
    }
    
//    private void initConn(Connection conn, Factory rawXmlFactory) {
//        throw new InternalError("not imp: initConn()");
//    }



    /** 
     * private c'tor.  inherits factory and xpath table.
     * Caller wil fill in other stuff
     */
//    private CLDRDBSource(CLDRFile.Factory nFactory) {
//            rawXmlFactory = nFactory; 
////            xpt = nXpt; 
//    }
//    /**
//     * Bootstrap Factory function for internal use. 
//     * @param theDir directory for XML data
//     * @param xpt XPathTable to use
//     * @param localeID locale id to use
//     * @param conn the database connection (shared)
//     * @param user the user to create for
//     */
//    public static CLDRDBSource createInstance(String theDir, XPathTable xpt, CLDRLocale localeID, Connection conn,
//        UserRegistry.User user) {
//        return createInstance(theDir, xpt, localeID, conn, user, false);
//    }
//    /**
//     * Factory function for internal use
//     * @param theDir directory for XML data
//     * @param xpt XPathTable to use
//     * @param locale locale id to use
//     * @param conn the database connection (shared)
//     * @param user the user to create for
//     * @param finalData true if to only return final (vettd) data
//     */
//    public static CLDRDBSource createInstance(String theDir, XPathTable xpt, CLDRLocale locale,
//            Connection conn, UserRegistry.User user, boolean finalData) {
//        CLDRFile.Factory afactory = CLDRFile.SimpleFactory.make(theDir,".*");
//        CLDRDBSource result =  new CLDRDBSource(afactory, xpt);
//        result.dir = theDir;
//        result.setLocaleID(locale.toString());
//        result.initConn(conn, afactory);
//        result.user = user;
//        result.finalData = finalData;
//        return result;
//    }
    
    

    public CLDRDBSource(CLDRLocale locale, boolean finalData) {
        this.finalData=finalData;
        this.setLocaleAndValidate(locale.toString());
    }



//    public void vettingMode(Vetting v) {
//        vetting = v;
//    }
    
    /**
     * Cloner. Shares the DB connection. 
     */
	public Object clone() {
        try {
            CLDRDBSource result = (CLDRDBSource) super.clone();
            // copy junk
//            result.xpath_comments = xpath_comments; // TODO: clone it.
            // Copy SHARED things
//            result.xpt = xpt; 
//            result.dir = dir;
            result.user = user;
//            result.conn = conn;  // gets set twice. but don't call initConn because other fields are still valid if it's just a clone.
//            result.rawXmlFactory = rawXmlFactory;
//            result.stmts = stmts;
//            result.srcHash = srcHash;
            result.aliasTable = aliasTable;
            result.finalData = finalData;
//            result.vetting = vetting;
            result.makeHash = makeHash;
            // do something here?
            return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}
	}
    
    /**
     * @see com.ibm.icu.util.Freezable#freeze()
     */
    // Lockable things
    public Object freeze() {
        locked = true;
        return this;
    }
    
    
    // TODO: remove this, implement as iterator( stringPrefix)
    /**
     * get the keyset over a prefix
     */
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
    
    /*
    
    Deprecated - getByType turned out not to be useful
    
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
    */
    
    
    /**
     * Add new data to the next sequentially available slot. 
     * This does perform a linear search, however it is only active when we are adding data, so should not
     * be an issue. 
     * @param file the CLDRFile  (unused)
     * @param locale the locale used
     * @param fullXpathMinusAlt  the xpath being added, with any "alt=" omitted. (alt is synthesized separately)
     * @param altType the 'type' portion of alt, such as "variant"
     * @param altProposedPrefix if it is a proposed alt, this is "proposed"
     * @param submitterId # of the submitter
     * @param value the value being added
     * @param refs ID of any references being added.
     * @return the entire altProposed which succeeded or NULL/throw for failure.
     * @deprecated do not use
     */
    public String addDataToNextSlot(CLDRFile file, CLDRLocale locale, String fullXpathMinusAlt, 
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
           // String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);  // removes  @used=  etc
           // if(!xpath.equals(rawXpath)) {
           //     logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
           // }
            String xpath = fullXpathMinusAlt + altTag;
            String oxpath = xpath+refStr+"[@draft=\"unconfirmed\"]";
            int xpid = xpt.getByXpath(xpath);
            
            // Check to see if this slot is in use.
            synchronized(conn) {
                try {
                    stmts.queryValue.setString(1,locale.toString());
                    stmts.queryValue.setInt(2,xpid);
                    ResultSet rs = stmts.queryValue.executeQuery();
                    if(rs.next()) {
                        // already taken..
                        //logger.info("Taken: " + altProposed);
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
                    stmts.insert.setString(2,locale.toString());
                    stmts.insert.setInt(3,srcId); // assumes homogenous srcId
                    stmts.insert.setInt(4,oxpid);
                    SurveyMain.setStringUTF8(stmts.insert, 5, value); // stmts.insert.setString(5,value);
                    stmts.insert.setString(6,eType);
                    stmts.insert.setString(7,eAlt);
                    stmts.insert.setInt(8,txpid); // tiny
                    stmts.insert.setInt(9,submitterId); // submitter
                    int base_xpid = xpt.xpathToBaseXpathId(xpath);
                    stmts.insert.setInt(10, base_xpid);
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
     * Add a new reference to the next sequentially available slot. 
     * This does perform a linear search, however it is only active when we are adding data, so should not
     * be an issue. 
     * @param file the CLDRFile (unused)
     * @param locale localeID added to
     * @param submitterId the submitter's numerical ID
     * @param value value of the reference
     * @param uri optional URI of the reference
     * @return the entire altProposed which succeeded or NULL/throw for failure.
     * @deprecated do not use
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
            String rawXpath = "//ldml/references/reference[@type=\""+type+"\"]"+uristr+"[@draft=\"unconfirmed\"]";
            logger.info("addDataToNextSlot:  rawXpath = " + rawXpath);
            String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);
            if(!xpath.equals(rawXpath)) {
                logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
            }
            String oxpath = xpath+uristr+"[@draft=\"unconfirmed\"]";
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
            
            /* todo - have a special func to find this */
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
                    SurveyMain.setStringUTF8(stmts.insert, 5, value); // stmts.insert.setString(5,value);
                    stmts.insert.setString(6,eType);
                    stmts.insert.setString(7,eAlt);
                    stmts.insert.setInt(8,txpid); // tiny
                    stmts.insert.setInt(9,submitterId); // submitter
                    int base_xpid = xpt.xpathToBaseXpathId(xpath);
                    stmts.insert.setInt(10, base_xpid);
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

    public String getSourceRevision(CLDRLocale locale) {
        int id = getSourceId(tree, locale.toString());
        if(id != -1) {
            return getSourceRevision(id);
        } else {
            return "[Unknown source:"+locale.toString()+"]";
        }
    }

    static boolean complained_about_slower_api= false;
    static protected void complain_about_slower_api() {
        if(!complained_about_slower_api) {
            complained_about_slower_api = true;
            System.err.println("CLDRDBSourceFactory: Note: instead of CLDRDBSource.putValueAtDPath() and CLDRDBSource.putFullPathAtDPath(), use CLDRDBSource.putValueAtPath().");
        }
        return;
    }

}
