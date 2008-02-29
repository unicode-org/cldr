//
//  Vetting.java
//  fivejay
//
//  Created by Steven R. Loomis on 04/04/2006.
//  Copyright 2006-2008 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;
import java.lang.ref.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import com.ibm.icu.util.ULocale;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CoverageLevel;

import com.ibm.icu.dev.test.util.ElapsedTimer;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
/**
 * This class does the calculations for which item wins a vetting,
 * and also manages some notifications,
 * and also records which votes are cast
 */
public class Vetting {

    private static String EMPTY_STRING = "";

    private static java.util.logging.Logger logger;
    
    public static final String CLDR_RESULT = "cldr_result"; /** constant for table access **/
    public static final String CLDR_VET = "cldr_vet"; /** constant for table access **/
    public static final String CLDR_INTGROUP = "cldr_intgroup"; /** constant for table access **/
    public static final String CLDR_STATUS = "cldr_status"; /** constant for table access **/
    public static final String CLDR_OUTPUT = "cldr_output"; /** constant for table access **/
    public static final String CLDR_ORGDISPUTE = "cldr_orgdispute"; /** constant for table access **/
    public static final String CLDR_ALLPATHS = "cldr_allpaths"; /** constant for table access **/
    
    
    public static final int VET_EXPLICIT = 0; /** 0: vote was explicitly made by user **/
    public static final int VET_IMPLIED  = 1; /** 1: vote was implicitly made by user as a result of new data entry **

    /* 
        0: no votes (and some draft data) - NO resolution
        1: insufficient votes - NO resolution
        A: admin override, not unanimous
        T: TC majority, not unanimous
        U: normal, unanimous
        N: No new data was entered.  Resolved item is the original, nondraft data.
    */
    public static final int RES_NO_VOTES        = 1;      /** 1:0 No votes for data (and some draft data) - NO resolution **/
    public static final int RES_INSUFFICIENT    = 2;      /** 2:I Not enough votes **/
    public static final int RES_ERROR           = 4;      /** 2:X Error-in-original **/
    public static final int RES_DISPUTED        = 8;      /** 4:D disputed item **/

    public static final int RES_BAD_MAX = RES_DISPUTED;  /** Data is OK (has a valid xpath in final data) if (type > RES_BAD_MAX) **/
    public static final int RES_BAD_MASK  = RES_NO_VOTES|RES_INSUFFICIENT|RES_ERROR|RES_DISPUTED;  /** bitmask for testing 'BAD' (invalid) **/
    
    
    public static final int RES_ADMIN           = 64;   /** 8:A Admin override **/
    public static final int RES_TC              = 128;  /** 16:T  TC override (or, TC voted at least..) **/
    public static final int RES_GOOD            = 256;  /** 32:G good - normal **/
    public static final int RES_UNANIMOUS       = 512;  /** 64:U  unamimous **/
    public static final int RES_NO_CHANGE       = 1024; /** 128:N no new data **/
    public static final int RES_REMOVAL         = 2048; /** 256: result: removal **/

    public static final String RES_LIST = "0IXD??ATGUNR"; /** list of strings for type 2**n.  @see typeToStr **/
	public static final String TWID_VET_VERBOSE = "Vetting_Verbose"; /** option string for toggling verbosity in vetting **/
	boolean VET_VERBOSE=false; /** option boolean for verbose vetting, defaults to false **/
	static boolean MARK_NO_DISQUALIFY = true; // mark items with errors, don't disqualify
    
    /**
     * Convert a vetting type to a string
     * @param t a type
     * @return string format
     **/
    public static String typeToStr(int t) {
        if(t==0) {
            return "z";
        }
        String rs = "";
        for(int i=0;i<RES_LIST.length();i++) {
            if(0!=(t&(1<<(i)))) {
                rs=rs+RES_LIST.charAt(i);
            }
        }
        return rs;
    }
    
    /** 
     * Called by SurveyMain to create the vetting table
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @param isNew  true if should CREATE TABLEs
     * @return vetting service object
     */
    public static Vetting createTable(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain sm) throws SQLException {
        Vetting reg = new Vetting(xlogger,ourConn,sm);
        reg.setupDB(); // always call - we can figure it out.
        reg.initStmts();
//        logger.info("Vetting DB: Created.");

		// initialize twid parameters
		reg.VET_VERBOSE=sm.twidBool(TWID_VET_VERBOSE);

        return reg;
    }

    /**
     * Internal Constructor for creating Vetting objects
     */
    private Vetting(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain ourSm) {
        logger = xlogger;
        conn = ourConn;
        sm = ourSm;
    }
    
    /**
     * Called by SurveyMain to shutdown
     */
    public void shutdownDB() throws SQLException {
        SurveyMain.closeDBConnection(conn);
    }

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException
    {
        synchronized(conn) {
            String sql = null;
//            logger.info("Vetting DB: initializing...");

            // remove allpaths table if present
            if(!sm.hasTable(conn, CLDR_ALLPATHS)) {
                logger.info("Vetting DB: setting up " + CLDR_ALLPATHS);
                Statement s = conn.createStatement();
                sql = "create table " + CLDR_ALLPATHS + " (base_xpath INT NOT NULL , unique(base_xpath))";
                logger.info("Vet: " + sql);
                s.execute(sql);
                sql = ("CREATE UNIQUE INDEX unique_xpath on " + CLDR_ALLPATHS +" (base_xpath)");
                logger.info("Vet: " + sql);
                s.execute(sql);
                logger.info("unique index created " + CLDR_ALLPATHS);
                s.close();
                conn.commit();
            } else {
                logger.info("Vetting DB: zapping " + CLDR_ALLPATHS);
                Statement s = conn.createStatement();
                sql = "delete from " + CLDR_ALLPATHS;
                int zapcnt = s.executeUpdate(sql);
                logger.info("Vet: " + sql + " ("+zapcnt+" removed)");
                s.close();
                conn.commit();
            }
            if(!sm.hasTable(conn, CLDR_RESULT)) {
                logger.info("Vetting DB: setting up " + CLDR_RESULT);
                Statement s = conn.createStatement();
                sql = "create table " + CLDR_RESULT + " (id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                                                        "locale VARCHAR(20) NOT NULL, " +
                                                        "base_xpath INT NOT NULL, " +
                                                        "result_xpath INT, " +
                                                        "modtime TIMESTAMP, " +
                                                        "type SMALLINT NOT NULL, "+
                                                      /*"old_result_xpath INT, "+
                                                        "old_modtime INT, "+
                                                        "old_type SMALLINT, "+*/
                                                        "unique(locale,base_xpath))";
                logger.info("Vet: " + sql);
                s.execute(sql);
                sql = ("CREATE UNIQUE INDEX unique_xpath on " + CLDR_RESULT +" (locale,base_xpath)");
                logger.info("Vet: " + sql);
                s.execute(sql);
                logger.info("unique index created " + CLDR_RESULT);
//                s.execute("CREATE INDEX "+CLDR_RESULT+"_loc_res on " + CLDR_RESULT +"(locale,result_xpath)");
                s.close();
                conn.commit();
            }
            if(!sm.hasTable(conn, CLDR_VET)) {
                logger.info("Vetting DB: setting up " + CLDR_VET);
                Statement s = conn.createStatement();
                s.execute("create table " + CLDR_VET + "(id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                                                        "locale VARCHAR(20) NOT NULL, " +
                                                        "submitter INT NOT NULL, " +
                                                        "base_xpath INT NOT NULL, " +
                                                        "vote_xpath INT NOT NULL, " +
                                                        "type SMALLINT NOT NULL, "+
                                                        "modtime TIMESTAMP, " +
                                                        "old_vote_xpath INT, "+
                                                        "old_modtime TIMESTAMP, "+
                                                        "unique(locale,submitter,base_xpath))");
                s.execute("CREATE UNIQUE INDEX unique_xpath on " + CLDR_VET +" (locale,submitter,base_xpath)");
                s.execute("CREATE INDEX "+CLDR_VET+"_loc_base on " + CLDR_VET +"(locale,base_xpath)");
                s.close();
                conn.commit();
            }
            if(!sm.hasTable(conn, CLDR_STATUS)) {
                logger.info("Vetting DB: setting up " + CLDR_STATUS);
                Statement s = conn.createStatement();
                s.execute("create table " + CLDR_STATUS + " " +
                                                        "(locale VARCHAR(20) NOT NULL, " +
                                                        "type SMALLINT NOT NULL, "+
                                                        "modtime TIMESTAMP NOT NULL, " +
                                                        "unique(locale))");
                s.execute("CREATE UNIQUE INDEX "+CLDR_STATUS+"_unique_loc on " + CLDR_STATUS +" (locale)");
                s.close();
                conn.commit();
            }

            if(!sm.hasTable(conn, CLDR_INTGROUP)) {
                logger.info("Vetting DB: setting up " + CLDR_INTGROUP);
                Statement s = conn.createStatement();
                s.execute("create table " + CLDR_INTGROUP + " " +
                                                        "(intgroup VARCHAR(20) NOT NULL, " +
                                                        "last_sent_nag TIMESTAMP," +
                                                        "last_sent_update TIMESTAMP," +
                                                        "unique(intgroup))");
                s.execute("CREATE UNIQUE INDEX "+CLDR_INTGROUP+"_unique_loc on " + CLDR_INTGROUP +" (intgroup)");
                s.close();
                conn.commit();
            }

			String theTable = CLDR_OUTPUT;
            if(!sm.hasTable(conn, theTable)) {
                logger.info("Vetting DB: setting up " + theTable);
                Statement s = conn.createStatement();
                s.execute("create table " + theTable + " " +
                                                        "(locale VARCHAR(20) NOT NULL, " +
                                                        "base_xpath INT NOT NULL," +
                                                        "output_xpath INT NOT NULL," +
                                                        "output_full_xpath INT NOT NULL," +
                                                        "data_xpath INT NOT NULL, " +
                                                        "status SMALLINT)");
                s.execute("CREATE INDEX "+theTable+"_fetchem on "   + theTable +" (locale,base_xpath)");
                s.execute("CREATE INDEX "+theTable+"_fetchfull on " + theTable +" (locale,output_xpath)");
                s.close();
                conn.commit();
            }

			theTable = CLDR_ORGDISPUTE;
            if(!sm.hasTable(conn, theTable)) {
                logger.info("Vetting DB: setting up " + theTable);
                Statement s = conn.createStatement();
                s.execute("create table " + theTable + " " +
                                                        "(org varchar(256) not null, " +
														"locale VARCHAR(20) not null, " +
                                                        "base_xpath INT not null, unique(org,locale,base_xpath))");
                s.execute("CREATE UNIQUE INDEX "+theTable+"_U on " + theTable +" (org,locale,base_xpath)");
                s.execute("CREATE INDEX "+theTable+"_fetchem on " + theTable +" (locale,base_xpath)");
                s.close();
                conn.commit();
            }
        }
    }
    
    /**
     * the Vetting sql connection
     */
    public Connection conn = null;
    
    /**
     * alias to the SurveyMain
     * @see SurveyMain
     */
    SurveyMain sm = null;
    
    /**
     * for future use - reports on vetting statistics
     */
    public String statistics() {
        return "Vetting: nothing to report";
    }
    
    /**
     * prepare statements for this connection 
     **/ 
    public PreparedStatement prepareStatement(String name, String sql) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
        } catch ( SQLException se ) {
            String complaint = "Vetter:  Couldn't prepare " + name + " - " + SurveyMain.unchainSqlException(se) + " - " + sql;
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
        return ps;
    }
    
    PreparedStatement missingImpliedVotes = null;
    PreparedStatement dataByUserAndBase = null;
    PreparedStatement insertVote = null;
    PreparedStatement queryVote = null;
    PreparedStatement queryValue = null;
    PreparedStatement queryVoteForXpath = null;
    PreparedStatement queryVoteForBaseXpath = null;
    PreparedStatement missingResults = null;
    PreparedStatement missingResults2 = null;
    PreparedStatement rmVote = null;
    PreparedStatement dataByBase = null;
    PreparedStatement insertResult = null;
    PreparedStatement queryResult = null;
    PreparedStatement countResultByType = null;
    PreparedStatement queryVoteId = null;
    PreparedStatement updateVote = null;
    PreparedStatement rmResult = null;
    PreparedStatement rmResultAll = null;
    PreparedStatement rmResultLoc = null;
    PreparedStatement queryTypes = null;
    PreparedStatement insertStatus = null;
    PreparedStatement updateStatus = null;
    PreparedStatement queryStatus = null;
    PreparedStatement staleResult = null;
    PreparedStatement updateResult = null;
    PreparedStatement intQuery = null;
    PreparedStatement intAdd = null;
    PreparedStatement intUpdateNag = null;
    PreparedStatement intUpdateUpdate = null;
    PreparedStatement listBadResults= null;
	PreparedStatement highestVetter = null;
    PreparedStatement lookupByXpath = null;

	
	// CLDR_OUTPUT
	PreparedStatement outputDelete = null;
	PreparedStatement outputInsert = null;
    PreparedStatement outputQueryStatus = null;
					// outputQuery will be in CLDRDBSource, the consumer
	// CLDR_ORGDISPUTE
	PreparedStatement orgDisputeDelete = null;
	PreparedStatement orgDisputeInsert = null;
    
	PreparedStatement orgDisputeLocs = null;  // org -> loc, #
	PreparedStatement orgDisputePaths = null; // org/locale -> paths
	PreparedStatement orgDisputePathCount = null; // org/locale -> paths
	PreparedStatement orgDisputeQuery = null; // org/locale/base  - t/f
					// query?

	PreparedStatement allpathsAdd = null; // org/locale -> paths
                    
    PreparedStatement googData = null;
	
    /**
     * initialize prepared statements
     */
    private void initStmts() throws SQLException {
        synchronized(conn) {
            missingImpliedVotes = prepareStatement("missingImpliedVotes", 
            "select distinct CLDR_DATA.submitter,CLDR_DATA.base_xpath from CLDR_DATA WHERE "+
                "(CLDR_DATA.submitter is not null)AND(locale=?) AND NOT EXISTS ( SELECT * from CLDR_VET where "+
                    "(CLDR_DATA.locale=CLDR_VET.locale) AND (CLDR_DATA.base_xpath=CLDR_VET.base_xpath) AND "+
                    "(CLDR_DATA.submitter=CLDR_VET.submitter) )");
            dataByUserAndBase = prepareStatement("dataByUserAndBase",
                "select CLDR_DATA.XPATH,CLDR_DATA.ALT_TYPE from CLDR_DATA where submitter=? AND base_xpath=? AND locale=?");
            dataByBase = prepareStatement("dataByBase", /*  1:locale, 2:base_xpath  ->  stuff */
                "select xpath,origxpath,alt_type,value FROM CLDR_DATA WHERE " + 
                    "(locale=?) AND (base_xpath=?)");
            insertVote = prepareStatement("insertVote",
                "insert into CLDR_VET (locale,submitter,base_xpath,vote_xpath,type,modtime) values (?,?,?,?,?,CURRENT_TIMESTAMP)");
            queryVote = prepareStatement("queryVote",
                "select CLDR_VET.vote_xpath from CLDR_VET where CLDR_VET.locale=? AND CLDR_VET.submitter=? AND CLDR_VET.base_xpath=?");
            rmVote = prepareStatement("rmVote",
                "delete from CLDR_VET where CLDR_VET.locale=? AND CLDR_VET.submitter=? AND CLDR_VET.base_xpath=?");
            queryVoteId = prepareStatement("queryVoteId",
                "select CLDR_VET.id from CLDR_VET where CLDR_VET.locale=? AND CLDR_VET.submitter=? AND CLDR_VET.base_xpath=?");
            updateVote = prepareStatement("updateVote",
                "update CLDR_VET set vote_xpath=?, type=?, modtime=CURRENT_TIMESTAMP where id=?");
            queryVoteForXpath = prepareStatement("queryVoteForXpath",
                "select CLDR_VET.submitter from CLDR_VET where CLDR_VET.locale=? AND CLDR_VET.vote_xpath=?");
            queryVoteForBaseXpath = prepareStatement("queryVoteForBaseXpath",
                "select CLDR_VET.submitter,CLDR_VET.vote_xpath from CLDR_VET where CLDR_VET.locale=? AND CLDR_VET.base_xpath=?");
            missingResults = prepareStatement("missingResults", /*  1:locale  ->  1: base_xpath */
                "select distinct CLDR_DATA.base_xpath from CLDR_DATA WHERE (locale=?) AND NOT EXISTS ( SELECT * from CLDR_RESULT where (CLDR_DATA.locale=CLDR_RESULT.locale) AND (CLDR_DATA.base_xpath=CLDR_RESULT.base_xpath)  )");
            missingResults2 = prepareStatement("missingResults2", /*  1:locale  ->  1: base_xpath */
                "select distinct CLDR_ALLPATHS.base_xpath from CLDR_ALLPATHS WHERE NOT EXISTS ( SELECT * from CLDR_RESULT where (CLDR_RESULT.locale=?) AND (CLDR_ALLPATHS.base_xpath=CLDR_RESULT.base_xpath)  )");
            insertResult = prepareStatement("insertResult", 
                "insert into CLDR_RESULT (locale,base_xpath,result_xpath,type,modtime) values (?,?,?,?,CURRENT_TIMESTAMP)");
            rmResult = prepareStatement("rmResult", 
                "delete from CLDR_RESULT where (locale=?)AND(base_xpath=?)");
            rmResultAll = prepareStatement("rmResultAll", 
                "delete from CLDR_RESULT");
            rmResultLoc = prepareStatement("rmResultLoc", 
                "delete from CLDR_RESULT where locale=?");
            queryResult = prepareStatement("queryResult",
                "select CLDR_RESULT.result_xpath,CLDR_RESULT.type from CLDR_RESULT where (locale=?) AND (base_xpath=?)");
            countResultByType = prepareStatement("countResultByType",
                "select COUNT(base_xpath) from CLDR_RESULT where locale=? AND type=?");
            listBadResults = prepareStatement("listBadResults",
                "select base_xpath,type from CLDR_RESULT where locale=? AND type<="+RES_BAD_MAX);
            queryTypes = prepareStatement("queryTypes",
                "select distinct CLDR_RESULT.type from CLDR_RESULT where locale=?");
            insertStatus = prepareStatement("insertStatus",
                "insert into CLDR_STATUS (type,locale,modtime) values (?,?,CURRENT_TIMESTAMP)");
            updateStatus = prepareStatement("updateStatus",
                "update CLDR_STATUS set type=?,modtime=CURRENT_TIMESTAMP where locale=?");
            queryStatus = prepareStatement("queryStatus",
                "select type from CLDR_STATUS where locale=?");
            staleResult = prepareStatement("staleResult",
                "select CLDR_RESULT.id,CLDR_RESULT.base_xpath from CLDR_RESULT where CLDR_RESULT.locale=? AND exists (select * from CLDR_VET where (CLDR_VET.base_xpath=CLDR_RESULT.base_xpath) AND (CLDR_VET.locale=CLDR_RESULT.locale) AND (CLDR_VET.modtime>CLDR_RESULT.modtime))");
            updateResult = prepareStatement("updateResult",
                "update CLDR_RESULT set result_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where base_xpath=? and locale=?");
            intQuery = prepareStatement("intQuery",
                "select last_sent_nag,last_sent_update from CLDR_INTGROUP where intgroup=?");
            intAdd = prepareStatement("intAdd",
                "insert into CLDR_INTGROUP (intgroup) values (?)");
            intUpdateNag = prepareStatement("intUpdateNag",
                "update CLDR_INTGROUP  set last_sent_nag=CURRENT_TIMESTAMP where intgroup=?");
            intUpdateUpdate = prepareStatement("intUpdateUpdate",
                "update CLDR_INTGROUP  set last_sent_update=CURRENT_TIMESTAMP where intgroup=?");
            queryValue = prepareStatement("queryValue",
                "SELECT value,origxpath FROM " + CLDRDBSource.CLDR_DATA +
                        " WHERE locale=? AND xpath=?");
			highestVetter = prepareStatement("highestVetter",
				"select CLDR_USERS.userlevel from CLDR_VET,CLDR_USERS where CLDR_USERS.id=CLDR_VET.submitter AND CLDR_VET.locale=? AND CLDR_VET.vote_xpath=? ORDER BY CLDR_USERS.userlevel");
				
			// CLDR_OUTPUT
			outputDelete = prepareStatement("outputDelete", // loc, basex
				"delete from CLDR_OUTPUT where locale=? AND base_xpath=?");
			outputInsert = prepareStatement("outputInsert", // loc, basex, outx, outFx, datax
				"insert into CLDR_OUTPUT (locale, base_xpath, output_xpath, output_full_xpath, data_xpath, status) values (?,?,?,?,?,?)");
			outputQueryStatus = prepareStatement("outputQueryStatus", // loc, basex, outx, outFx, datax
				"select status from CLDR_OUTPUT where locale=? AND output_xpath=?");

			// CLDR_ORGDISPUTE
			orgDisputeDelete = prepareStatement("orgDisputeDelete", // loc, basex
				"delete from CLDR_ORGDISPUTE where locale=? AND base_xpath=?");
			orgDisputeInsert = prepareStatement("orgDisputeInsert", // org, locale, base_xpath
				"insert into CLDR_ORGDISPUTE (org, locale, base_xpath) values (?,?,?)");
                
            orgDisputeLocs = prepareStatement("orgDisputeLocs", 
                "select locale,count(*) from CLDR_ORGDISPUTE where org=? group by locale  order by locale");
            orgDisputePaths = prepareStatement("orgDisputePaths", 
                "select base_xpath from CLDR_ORGDISPUTE where org=? AND locale=?");
            orgDisputePathCount = prepareStatement("orgDisputePathCount", 
                "select COUNT(base_xpath) from CLDR_ORGDISPUTE where org=? AND locale=?");
            orgDisputeQuery = prepareStatement("orgDisputeQuery", 
                "select * from CLDR_ORGDISPUTE where org=? AND locale=? and base_xpath=?");
                
            googData = prepareStatement("googData",
                "select xpath,origxpath,value from CLDR_DATA where alt_type='proposed-x650' and locale=? and base_xpath=?");
                
            allpathsAdd = prepareStatement("allpathsAdd",
                "insert into CLDR_ALLPATHS (base_xpath) values (?)");
        }
    }
    
    /**
     * update all implied votes ( where a user has entered a new data item, they are assumed to be voting for that item )
     * @return number of votes that were updated
     */
    public int updateImpliedVotes() {
        ElapsedTimer et = new ElapsedTimer();
        System.err.println("updating implied votes..");
        File inFiles[] = sm.getInFiles();
        int tcount = 0;
        int lcount = 0;
        int nrInFiles = inFiles.length;
        for(int i=0;i<nrInFiles;i++) {
            String fileName = inFiles[i].getName();
            int dot = fileName.indexOf('.');
            String localeName = fileName.substring(0,dot);
            if((i%100)==0) {
                System.err.println(localeName + " - "+i+"/"+nrInFiles);
            }
            ElapsedTimer et2 = new ElapsedTimer();
            int count = updateImpliedVotes(localeName);
            tcount += count;
            if(count>0) {
                lcount++;
                System.err.println("updateImpliedVotes("+localeName+") took " + et2.toString());
            } else {
                // no reason to print it.
            }
        }
        System.err.println("Done updating "+tcount+" implied votes ("+lcount + " locales). Elapsed:" + et.toString());
        return tcount;
    }
    
    /**
     * update implied votes for one locale
     * @param locale which locale 
     * @return locale count
     */
    public int updateImpliedVotes(String locale) {
        int count = 0;
        int updates = 0;

        // we use the ICU comparator, but invert the order.
        Map items = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2) {
                return myCollator.compare(o2.toString(),o1.toString());
            }
        });

        synchronized(conn) {
            try {
                dataByUserAndBase.setString(3, locale);
                missingImpliedVotes.setString(1,locale);
                insertVote.setString(1,locale);
                insertVote.setInt(5,VET_IMPLIED);
                ResultSet rs = missingImpliedVotes.executeQuery();
                while(rs.next()) {
                    count++;
                    int submitter = rs.getInt(1);
                    int base_xpath = rs.getInt(2);
                    
                    // debug?
                    
                    //UserRegistry.User submitter_who = sm.reg.getInfo(submitter);
                    //String base_xpath_str = sm.xpt.getById(base_xpath);
                    //System.err.println(locale + " " + submitter_who.email + " " + base_xpath_str);
                    
                    
                    dataByUserAndBase.setInt(1,submitter);
                    dataByUserAndBase.setInt(2,base_xpath);
                    
                    insertVote.setInt(2,submitter);
                    insertVote.setInt(3,base_xpath);
                    ResultSet subRs = dataByUserAndBase.executeQuery();
                    //TODO: if this is too slow, optimize for the single-entry case.
                    items.clear();
                    while(subRs.next()) {
                        int xpath = subRs.getInt(1);
                        String altType = subRs.getString(2);
                    
                        
                        //System.err.println(".. " + altType + " = " + sm.xpt.getById(xpath));
                        if(altType == null) {
                            altType = "";
                        }
                        items.put(altType, new Integer(xpath));
                    }
                    if(items.isEmpty()) {
                        UserRegistry.User submitter_who1 = sm.reg.getInfo(submitter);
                        String base_xpath_str1 = sm.xpt.getById(base_xpath);
                        System.err.println("ERR: NO WINNER for ipath " + locale + " " + submitter_who1.email + "#"+base_xpath+" " + base_xpath_str1);
                    } else {
                        int winningXpath = ((Integer)items.values().iterator().next()).intValue();
                        //System.err.println("Winner= " + items.keySet().iterator().next() + " = " + sm.xpt.getById(winningXpath));
                        insertVote.setInt(4,winningXpath);
                        
                        //  "insert into CLDR_VET (locale,submitter,base_xpath,vote_xpath,type,modtime) values (?,?,?,?,?,CURRENT_TIMESTAMP)");
                        //System.err.println(locale+","+submitter+","+base_xpath+","+winningXpath+","+VET_IMPLIED);
                        
                        int rows = insertVote.executeUpdate();
                        if(rows != 1) {
                            String complaint = "Vetter: while updating ["+locale+","+submitter+","+base_xpath+","+
                                winningXpath+","+VET_IMPLIED+"] - expected '1' row, got " + rows;
                            logger.severe(complaint);
                            throw new RuntimeException(complaint);
                        }
                    }
                }
                
                if(count>0) {
                    System.err.println("Updating " + count + " for " + locale);
                    conn.commit();
                }
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't update implied votes for  " + locale + " - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
            return count;
        }
    }
    
    /**
     * private utility to create a new collator which can sort numerically
     */
    static private Collator getOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance(new ULocale("root")));
        //rbc.setNumericCollation(true);
        return rbc;
    }
    
    /**
     * return the result of a particular contest
     * @param locale which locale
     * @param userid of the submitter
     * @param base_xpath base xpath id to check
     * @return result of vetting, RES_NO_VOTES, etc.
     * @see XPathTable
     */
    public int queryVote(String locale, int submitter, int base_xpath) {
        synchronized(conn) {
            try {
                queryVote.setString(1, locale);
                queryVote.setInt(2, submitter);
                queryVote.setInt(3, base_xpath);
                ResultSet rs = queryVote.executeQuery();
                while(rs.next()) {
                    int vote_xpath = rs.getInt(1);
                    return vote_xpath;
                }
                return -1;
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't query  votes for  " + locale + " - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
    }

    /**
     * get a Set consisting of all of the users which voted on a certain path
     * @param locale which locale
     * @param base_xpath string xpath to gather for
     * @return a Set of UserRegistry.User objects
     */
     
    public Set<UserRegistry.User> gatherVotes(String locale, String base_xpath) {
        int base_xpath_id = sm.xpt.getByXpath(base_xpath);
        synchronized(conn) {
            try {
                queryVoteForXpath.setString(1, locale);
                queryVoteForXpath.setInt(2, base_xpath_id);
                ResultSet rs = queryVoteForXpath.executeQuery();
               // System.err.println("Vf: " + base_xpath_id + " / " + base_xpath);
                Set<UserRegistry.User> result = null;
                while(rs.next()) {
                    if(result == null) {
                        result = new HashSet<UserRegistry.User>();
                    }
                    int vote_user = rs.getInt(1);
                   // System.err.println("...u#"+vote_user);
                    result.add(sm.reg.getInfo(vote_user));
                }
                return result;
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't query  users for  " + locale + " - " + base_xpath +" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
    }
    
    /**
     * The static collator to use
     */
    final Collator myCollator = getOurCollator();

    // update all 
    public int updateResults() {
        return updateResults(false);
    }
    /**
     * update all vetting results
     * @return the number of results changed
     */
    public boolean stopUpdating = false;
    
    public int updateResults(boolean removeFirst) {
        stopUpdating = false;
        try {
            sm.progressWhat = "vetting update";
            ElapsedTimer et = new ElapsedTimer();
            System.err.println("updating results... ***********************************");
            File inFiles[] = sm.getInFiles();
            int tcount = 0;
            int lcount = 0;
            int types[] = new int[1];
            int nrInFiles = inFiles.length;
            sm.progressMax = nrInFiles;
            for(int i=0;i<nrInFiles;i++) {
                sm.progressCount = i;
                // TODO: need a function for this.
                String fileName = inFiles[i].getName();
                int dot = fileName.indexOf('.');
                String localeName = fileName.substring(0,dot);
                //System.err.println(localeName + " - "+i+"/"+nrInFiles);
                ElapsedTimer et2 = new ElapsedTimer();
                types[0]=0;
                
                int count = updateResults(localeName,types, removeFirst);
                tcount += count;
                if(count>0) {
                    lcount++;
                    System.err.println("updateResults("+localeName+ " ("+count+" updated, "+typeToStr(types[0])+") - "+i+"/"+nrInFiles+") took " + et2.toString());
                } else {
                    // no reason to print it.
                }
                
                if(stopUpdating) {
                    stopUpdating = false;
                    System.err.println("**** Update aborted after ("+count+" updated, "+typeToStr(types[0])+") - "+i+"/"+nrInFiles);
                    break;
                }
            }
            System.err.println("Done updating "+tcount+" results votes ("+lcount + " locales). Elapsed:" + et.toString());
            System.err.println("******************** NOTE: updateResults() doesn't send notifications yet.");
            return tcount;
        } finally {
            sm.progressWhat = null; // clean up counter.
        }
    }
    
    /**
     * update the results of a specific locale, without caring what kind of results were had.  This is a convenience
     * function so you don't have to new up an array.
     * @param locale which locale
     * @return number of results changed
     */
    public int updateResults(String locale) {
        ElapsedTimer et2 = new ElapsedTimer();
        int type[] = new int[1];
        int count = updateResults(locale,type);
        System.err.println("Vetting Results for "+locale+ ":  "+count+" updated, "+typeToStr(type[0])+" - "+ et2.toString());
        return count;
    }
    
    private Set<Integer> gAllImportantXpaths = null;
    
    private synchronized Set<Integer> getAllImportantXpaths() {
        int updates = 0;
        if(gAllImportantXpaths == null) {
            ElapsedTimer et2 = new ElapsedTimer();
            Set<Integer> aSet  = new HashSet<Integer>();
            synchronized(conn) {
              //  CoverageLevel coverageLevel = new CoverageLevel();
                
                try {
                    for(Iterator<String> paths = sm.getBaselineFile().iterator();
                            paths.hasNext();) {
                        String path = paths.next();
                      //  CoverageLevel.Level level = coverageLevel.getCoverageLevel(path);
                      //  if (level == CoverageLevel.Level.UNDETERMINED) continue; // continue if we don't know what the status is
                      //  if (CoverageLevel.Level.MINIMAL.compareTo(level)<0) continue; // continue if too low
                        
                        int base_xpath = sm.xpt.getByXpath(path);
                        Integer xp = new Integer(base_xpath);
                        aSet.add(xp);
                        allpathsAdd.setInt(1,base_xpath);
                        updates+=allpathsAdd.executeUpdate();
                    }
                    gAllImportantXpaths = Collections.unmodifiableSet(aSet);
                    conn.commit();
                } catch ( SQLException se ) {
                    String complaint = "Vetter:  couldn't update CLDR_ALLPATHS" + " - " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    se.printStackTrace();
                    throw new RuntimeException(complaint);
                }
            }
            System.err.println("importantXpaths: calculated in " + et2 + " - " + gAllImportantXpaths.size() + ", "+updates+" updates");
        }
        return gAllImportantXpaths;
    }
    public int updateResults(String locale, int type[]) {
        return updateResults(locale, type, false);
    }
    
    /**
     * update the results of a specific locale, and return the bitwise OR of all types of results had
     * @param locale which locale
     * @param type an OUT parameter, must be a 1-element ( new int[1] ) array. input value doesn't matter. on output, 
     * contains the bitwise OR of all types of vetting results which were found.
     * @return number of results changed
     **/
    public int updateResults(String locale, int type[], boolean removeFirst) {
		VET_VERBOSE=sm.twidBool(TWID_VET_VERBOSE); // load user prefs: should we do verbose vetting?
        int ncount = 0; // new count
        int ucount = 0; // update count
        int updates = 0;
        int base_xpath=-1;
        // two lists here.
        //  #1  results that are missing (unique CLDR_DATA.base_xpath but no CLDR_RESULT).  Use 'insert' instead of 'update', no 'old' data.
        //  #2  results that are out of date (CLDR_VET with same base_xpath but later modtime.).  Use 'update' instead of 'insert'. (better yet, use an updatable resultset).
        
        Set<Race> racesToUpdate = new HashSet<Race>();
        
        ////Set<Integer> allPaths = getAllImportantXpaths();
        //Set<Integer> todoPaths = new HashSet<Integer>();
        //todoPaths.addAll(allPaths);
        
        synchronized(conn) {
            try {
            
                if(removeFirst) {            
                    rmResultLoc.setString(1, locale);
                    int del = rmResultLoc.executeUpdate();
                    System.err.println("** "+ locale + " - "+del+" results removed");
                    conn.commit();
                }            
                
                //dataByUserAndBase.setString(3, locale);
                missingResults.setString(1,locale);
                //insertVote.setString(1,locale);
                //insertVote.setInt(5,VET_IMPLIED);
                
                // for updateResults(id, ...)
                dataByBase.setString(1,locale);
                queryVoteForBaseXpath.setString(1,locale);
                insertResult.setString(1,locale);
                
                // Missing results...
                ResultSet rs = missingResults.executeQuery();
                while(rs.next()) {
                    ncount++;
                    base_xpath = rs.getInt(1);
                    
                    //todoPaths.remove(new Integer(base_xpath));
                    int rc = updateResults(-1, locale, base_xpath, racesToUpdate);

                    updates |= rc;
                }
                // out of date results..
                staleResult.setString(1,locale);
                rs = staleResult.executeQuery();  // id, base_xpath
                while(rs.next()) {
                    ucount++;
                    int id = rs.getInt(1);
                    base_xpath = rs.getInt(2);
                    
                    int rc = updateResults(id, locale, base_xpath, racesToUpdate);
                    //    System.err.println("*Updated id " + id + " of " + locale+":"+base_xpath);
                    updates |= rc;
                }

                int mcount=0;
/*
                missingResults2.setString(1,locale);
                rs = missingResults2.executeQuery();
                
                while(rs.next()) {
                    mcount++;
                    base_xpath = rs.getInt(1);
                    String base_xpath_str = sm.xpt.getById(base_xpath);

                    int rc = updateResults(-1, locale, base_xpath, racesToUpdate);
                    //    System.err.println("*Updated id " + id + " of " + locale+":"+base_xpath);
                    updates |= rc;

                    //System.err.println(locale+":"+base_xpath+" missing "+base_xpath_str);
                }
                if(mcount>0) {
                    System.err.println("Missing items in "+ locale + " - "+mcount);
                    conn.commit();
                }
*/

                // if anything changed, commit it
                if((ucount > 0) || (ncount > 0) || (mcount > 0)) {
                    int uscnt = updateStatus(locale, true);// update results
                    if(uscnt>0) {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to vote change");
                    } else {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to vote change??");
                    }
                    conn.commit();
                }
                
                // Now that we've committed, should be OK to look for errs.
                Set<Race> errorRaces = searchForErrors(racesToUpdate);
                
                if(!errorRaces.isEmpty()) {
                    correctErrors(errorRaces);
                    updates |= RES_ERROR;

                    // if anything changed, commit it
                    int uscnt = updateStatus(locale, true);// update results
                    if(uscnt>0) {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to error change");
                    } else {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to error change??");
                    }
                    conn.commit();
                }
                
                
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't update vote results for  " + locale + " - " + SurveyMain.unchainSqlException(se) + 
                    "base_xpath#"+base_xpath+" "+sm.xpt.getById(base_xpath);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
            type[0] = updates;
            return ncount + ucount;
        }
    }
    
    private Set<Race> searchForErrors(Set<Race> racesToUpdate) {
//        System.err.println(racesToUpdate.size() + " to check");
        Set<Race> errorRaces = new HashSet<Race>();
        
        for(Race r : racesToUpdate) {
            if(r.recountIfHadDisqualified()) {
//if(r.base_xpath==85942)                System.err.println("Had errs; " + r.locale + " / " + r.base_xpath);
                errorRaces.add(r);
            }
        }
        
        return errorRaces;
    }
    
    private int correctErrors(Set<Race> errorRaces) throws SQLException {
        int n = 0;
//        System.err.println(errorRaces.size() + " to correct");
        for(Race r : errorRaces) {
            r.updateDB();
            n++;
        }
        conn.commit();
//        System.err.println(n+" items corrected");
        return n;
    }

    /**
     * wash (prepare for next CLDR release) all votes
     * @return the number of data items changed
     */
    public int washVotes() {
        System.err.println("******************** washVotes() .");
        ElapsedTimer et = new ElapsedTimer();
        System.err.println("updating results...");
        File inFiles[] = sm.getInFiles();
        int tcount = 0;
        int lcount = 0;
        int types[] = new int[1];
        int nrInFiles = inFiles.length;
        for(int i=0;i<nrInFiles;i++) {
            // TODO: need a function for this.
            String fileName = inFiles[i].getName();
            int dot = fileName.indexOf('.');
            String localeName = fileName.substring(0,dot);
            //System.err.println(localeName + " - "+i+"/"+nrInFiles);
            ElapsedTimer et2 = new ElapsedTimer();
            types[0]=0;
            int count = washVotes(localeName/*,types*/);
            tcount += count;
            if(count>0) {
                lcount++;
                System.err.println("washVotes("+localeName+ " ("+count+" washed, "+typeToStr(types[0])+") - "+i+"/"+nrInFiles+") took " + et2.toString());
            } else {
                // no reason to print it.
            }
            
            /*
                if(count>0) {
                System.err.println("SRL: Interrupting.");
                break;
            }
            */
        }
        System.err.println("Done washing "+tcount+" in ("+lcount + " locales). Elapsed:" + et.toString());
        System.err.println("******************** NOTE: washVotes() doesn't send notifications yet.");
        return tcount;
    }
    
    /**
     * update the results of a specific locale, without caring what kind of results were had.  This is a convenience
     * function so you don't have to new up an array.
     * @param locale which locale
     * @return number of results changed
     */
     /*
    public int washVotes(String locale) {
        ElapsedTimer et2 = new ElapsedTimer();
        int type[] = new int[1];
        int count = washVotes(locale,type);
        System.err.println("Vetting Results for "+locale+ ":  "+count+" updated, "+typeToStr(type[0])+" - "+ et2.toString());
        return count;
    }*/
    
    /**
     * update the results of a specific locale, and return the bitwise OR of all types of results had
     * @param locale which locale
     * @param type an OUT parameter, must be a 1-element ( new int[1] ) array. input value doesn't matter. on output, 
     * contains the bitwise OR of all types of vetting results which were found.
     * @return number of results changed
     **/
    public int washVotes(String locale /*, int type[]*/) {
        int type[] = new int[1];
		VET_VERBOSE=sm.twidBool(TWID_VET_VERBOSE); // load user prefs: should we do verbose vetting?
        int ncount = 0; // new count
        int ucount = 0; // update count'
        int fcount = 0; // total thrash count
        int zcount = 0; // count of inner thrash
        int zocount = 0; // count of inner thrash
        
        int updates = 0;
        int base_xpath=-1;
        long lastU = System.currentTimeMillis();
        // two lists here.
        //  #1  results that are missing (unique CLDR_DATA.base_xpath but no CLDR_RESULT).  Use 'insert' instead of 'update', no 'old' data.
        //  #2  results that are out of date (CLDR_VET with same base_xpath but later modtime.).  Use 'update' instead of 'insert'. (better yet, use an updatable resultset).
        synchronized(conn) {
            try {
                Statement s3 = conn.createStatement();
                Statement s2 = conn.createStatement();
                Statement s = conn.createStatement();
                
                if(lookupByXpath == null) {
                    lookupByXpath = prepareStatement("lookupByXpath", 
                        "select xpath,value,source,origxpath from CLDR_DATA where base_xpath=? AND SUBMITTER is NULL AND locale=?");
                }
                
                lookupByXpath.setString(2,locale);
                int cachedBase = -1;
                int vettedValue = -1; // do we have a vetted value?
                Hashtable cachedProps = new Hashtable();

                try {
                    ULocale ulocale = new ULocale(locale);
                    //                        WebContext xctx = new WebContext(false);
                    //                        xctx.setLocale(locale);
                    sm.makeCLDRFile(sm.makeDBSource(sm.getDBConnection(),  null, ulocale));
                } catch(Throwable t) {
                    t.printStackTrace();
                    String complaint = ("Error loading: " + locale + " - " + t.toString() + " ...");
                    logger.severe("loading "+locale+": " + complaint);
                 //   ctx.println(complaint + "<br>" + "<pre>");
                //    ctx.print(t);
                //    ctx.println("</pre>");
                }


                int vetCleared = s3.executeUpdate("delete from CLDR_VET where locale='"+locale+"'");
                int resCleared = s3.executeUpdate("delete from CLDR_RESULT where locale='"+locale+"'");
                
                // clear RESULTS
                System.err.println(locale + " - cleared "+vetCleared + " from CLDR_VET");
                System.err.println(locale + " - cleared "+resCleared + " from CLDR_RESULT");
                
                ResultSet rs = s.executeQuery("select id,source,value,base_xpath,submitter from CLDR_DATA where SUBMITTER IS NOT NULL AND LOCALE='"+locale+"' order by base_xpath, source desc");
                System.err.println(" querying..");
                while(rs.next()) {
                   // ncount++;
                   fcount++;
                    int oldId = rs.getInt(1);
                    int oldSource = rs.getInt(2);
                    String oldValue = rs.getString(3);
                    base_xpath = rs.getInt(4);
                    int oldSubmitter = rs.getInt(5);
                    
                  //  if(base_xpath != 4008) {   continue;  }
                  
                    long thisU = System.currentTimeMillis();
                    
                    if((thisU-lastU)>5000) {
                        lastU = thisU;
                        System.err.println(locale + " - #"+fcount+ ", so far " + ncount+ " - ["+zcount+"/"+zocount+"]");
                    }
                    
                    if(cachedBase != base_xpath) {
                        zcount++;
                        cachedBase=base_xpath;
                        vettedValue=-1;
                        cachedProps.clear();
                        
                        lookupByXpath.setInt(1, base_xpath);
                        ResultSet rs2 = lookupByXpath.executeQuery();
                        while(rs2.next()) {
                            int newXpath = rs2.getInt(1);
                            String newValue = rs2.getString(2);
                            int newSource = rs2.getInt(3);
                            int newOXpath = rs2.getInt(4);
                            zocount++;
                            
                            if(newSource > oldSource) {
                                // content must be from a newer xml file than the most recent vote.
                                if((newXpath == base_xpath) && (newXpath == newOXpath)) { // 
                                    vettedValue = newXpath; // vetted - drop the other one
                                } else {
                                    cachedProps.put(newValue,new Integer(newXpath));
                                }
                            }
                        }
                        // -
                    }
                    
                    Integer ourProp = (Integer)cachedProps.get(oldValue);
                    //System.err.println("Wash:"+locale+" #"+oldId+"//"+base_xpath+" -> v"+vettedValue+" but props "+ cachedProps.size()+", , p"+(ourProp==null?"NULL":ourProp.toString()));
                    if((vettedValue==-1)&&(ourProp!=null)) {
                        // cast a vote for ourProp
                        vote(locale, base_xpath, oldSubmitter, ourProp.intValue(), VET_IMPLIED);
                    }
                    if((vettedValue != -1) || (cachedProps.size()>0)) {
                        // just, erase it
                        ncount += s3.executeUpdate("delete from CLDR_DATA where id="+oldId);
                    }
                    
                    // what to do?
                  //  updates |= rc;
                }

                    /*                
                // out of date
                staleResult.setString(1,locale);
                rs = staleResult.executeQuery();  // id, base_xpath
                while(rs.next()) {
                    ucount++;
                    int id = rs.getInt(1);
                    base_xpath = rs.getInt(2);
                    
                    int rc = updateResults(id, locale, base_xpath);
                    //    System.err.println("*Updated id " + id + " of " + locale+":"+base_xpath);
                    updates |= rc;
                }
                if(ucount > 0) {
                    int uscnt = updateStatus(locale, true);// update results
                    if(uscnt>0) {
                        System.err.println("updated " + uscnt + " statuses, due to vote change");
                    } else {
                        System.err.println("updated " + uscnt + " statuses, due to vote change??");
                    }
                    conn.commit();
                }
                */
                conn.commit();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't wash vote results for  " + locale + " - " + SurveyMain.unchainSqlException(se) + 
                    "base_xpath#"+base_xpath+" "+sm.xpt.getById(base_xpath);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
            type[0] = updates;
            System.err.println("Wash  : "+locale+" - count: "+ ncount + " ["+zcount+"/"+zocount+"]");
            System.err.println("Update: "+locale+" - count: " + updateResults(locale));
            return ncount + ucount;
        }
    }
	
	/** 
	 * Parameters used for vote tallying.
	 * Note that "*4" is because the original specification was in terms of (1/4) vote, 1 vote, 2 votes.. normalized by multiplying everything *4
	 */
	public static final int ADMIN_VOTE		= 64; // 2 * 4
	public static final int EXPERT_VOTE		= 8;  // 2 * 4
	public static final int EXISTING_VOTE	= 4;  // 1 * 4
	public static final int DEFAULT_ORG_VOTE= 4;  // 1 * 4
	public static final int VETTER_VOTE		= 4;  // 1 * 4
	public static final int STREET_VOTE		= 1;  // .25 * 4

    public static final int OUTPUT_MINIMUM = 0; // Don't even show losing items that aren't at least this.   [ was 4]

	public enum Status { 
		INDETERMINATE (-1),
		APPROVED (0),
		CONTRIBUTED (1),
		PROVISIONAL (2),
		UNCONFIRMED (3);
        
        Status(int n) {
            this.asInt = n;
            this.asAttribute = this.toString().toLowerCase();
        }
        
        int asInt;
        String asAttribute;
        
        int intValue() {
            return asInt;
        }
        
        String attribute() {
            return asAttribute;
        }
        
        public static Status find(int n) {
            for (Status s : EnumSet.allOf(Status.class)) {
                if(s.intValue() == n) {
                    return s;
                }
            }
            return INDETERMINATE;
        }
	}
	
	public int makeXpathId(String baseNoAlt, String altvariant, String altproposed, Status status) {
		return sm.xpt.getByXpath(makeXpath(baseNoAlt, altvariant, altproposed, status));
	}
	
	public String makeXpath(String baseNoAlt, String altvariant, String altproposed, Status status) {
		String out = baseNoAlt;
		
		if((altvariant != null) || (altproposed != null)) {
			out = out + "[@alt=\""+ LDMLUtilities.formatAlt(altvariant, altproposed) +"\"]";
		}

		if((status != Status.INDETERMINATE) && 
			(status != Status.APPROVED) ) { 
			String statustype = "indeterminate";
			switch(status) {
				case INDETERMINATE: statustype = "indeterminate"; break;
				case CONTRIBUTED: statustype = "contributed"; break;
				case PROVISIONAL: statustype = "provisional"; break;
				case UNCONFIRMED: statustype = "unconfirmed"; break;
				case APPROVED: statustype = "approved"; break;
			}
			
			out = out + "[@draft=\""+statustype+"\"]";
		}
		
		return out;
	}
    
    /**
     * Cache of default vote
     */
    private Map<String, Integer>  googleMap = new HashMap<String,Integer>();
     
    private int getDefaultWeight(String name, String locale) {
        if(name.equals("google")) {
            Integer weightInt = googleMap.get(locale);
            if(weightInt != null) {
                return weightInt.intValue();
            }
            int weight = 
                CoverageLevel.Level.getDefaultWeight(name, locale);
            googleMap.put(locale, weight);
            return weight;
        } else {
            return CoverageLevel.Level.getDefaultWeight(name, locale);
        }
    }
	
	/**
	 * Inner classes used for vote tallying 
	 */

	public static final String EXISTING_ITEM_NAME =  "Existing Item";

    /**
     * This class represents a particular item that can be voted for,
     * a single "contest" if you will.
     */
	public class Race {
		// The vote of a particular organization for this item.
		class Organization implements Comparable {
			String name; // org's name
			Chad vote = null; // the winning item: -1 for unknown
			int strength=0; // strength of the vote
			public boolean dispute = false;
            public boolean checked = false; // checked the vote yet?
			
			Set<Chad> votes = new TreeSet<Chad>(); // the set of chads
			
			public Organization(String name) {
				this.name = name;
			}
			
			/**
			 * Factory function - create an existing Vote
			 */
			public Organization(Chad existingItem) {
				name = EXISTING_ITEM_NAME;
				vote = existingItem;
				strength = EXISTING_VOTE;
			}
			
			// we have some interest in this race
			public void add(Chad c) {
				votes.add(c);
			}
            
            // this is the org's default vote. 
            public void setDefaultVote(Chad c, int withStrength) {
                //votes.add(c); // so it is in the list
                vote = c; // and it is the winning vote
                strength = withStrength;
            }
			
			// All votes have been cast, decide which one this org votes for.
			Chad calculateVote() {
				if(vote == null && !checked) {
                    checked = true;
					int lowest = UserRegistry.LIMIT_LEVEL;
					for(Chad c : votes) {
                        if(c.disqualified && !MARK_NO_DISQUALIFY) continue;
						int lowestUserLevel = UserRegistry.LIMIT_LEVEL;
						for(UserRegistry.User aUser : c.voters) {
							if(!aUser.org.equals(name)) continue;
							if(lowestUserLevel>aUser.userlevel) {
								lowestUserLevel = aUser.userlevel;
							}
						}
						if(lowestUserLevel<UserRegistry.LIMIT_LEVEL) {
							if(lowestUserLevel<lowest) {
								lowest = lowestUserLevel;
								vote = c;
							} else if (lowestUserLevel == lowest) {
								// Dispute.
								lowest = UserRegistry.NO_LEVEL;
								vote = null;
							}
						}
					}
					
					// now, rate based on lowest user level ( = highest rank )
					if(vote != null) {
						if(lowest <= UserRegistry.ADMIN) {
							strength = ADMIN_VOTE; // "2" * 4
						} else if(lowest <= UserRegistry.EXPERT) {
							strength = EXPERT_VOTE; // "2" * 4
						} else if(lowest <= UserRegistry.VETTER) {
							strength = VETTER_VOTE; // "1" * 4
						} else if(lowest <= UserRegistry.STREET) {
							strength = STREET_VOTE; // "1/4" * 4
							// TODO: liason / guest vote levels go here.
						} else {
							vote = null;  // no vote cast
						}
					} else if(lowest == -1) {
						strength = 0;
						dispute = true;
						vote = null;
					}
				}
				return vote;
			}
			
			public int compareTo(Object o) {
				if(o==this) { 
					return 0;
				}
				Organization other = (Organization)o;
				if(other.strength>strength) {
					return 1;
				} else if(other.strength < strength) {
					return -1;
				} else {
					return name.compareTo(other.name);
				}
			}
		}

		// All votes for a particular item
		class Chad implements Comparable {
			public int xpath = -1;
			public int full_xpath = -1;
			public Set<UserRegistry.User> voters = new HashSet<UserRegistry.User>(); //Set of users which voted for this.
			public Set<Organization> orgs = new TreeSet<Organization>(); // who voted for this?
			public int score = 0;
			public String value = null;
            
            public String refs = null;
            
            public Set<Organization> orgsDefaultFor = null; // if non-null: this Chad is the default choice for the org[s] involved, iff they didn't already vote.
            
            boolean disqualified;

			public Chad(int xpath, int full_xpath, String value) {
				this.xpath = xpath;
				this.full_xpath = full_xpath;
				this.value = value;
                
			}
            
            boolean checkDisqualified() {
                if(disqualified) {
                    return true;
                }
                if(value == null) {  // non existent item. ignore.
                    return false;
                }
                disqualified = test(locale, xpath, full_xpath, value); 
                if(disqualified  && !MARK_NO_DISQUALIFY) {
                    score = 0;
                    //if(/*sm.isUnofficial && */ base_xpath==83422) {
                    //    System.err.println("DISQ: " + locale + ":"+xpath + " ("+base_xpath+") - " + value);
                    //}
                }
                return disqualified;
            }
            
            /**
             * Call this if the item is a default vote for an organization
             */
            public void addDefault(Organization org) {
                // do NOT add to 'orgs'
                if(orgsDefaultFor == null) {
                    orgsDefaultFor = new HashSet<Organization>();
                }
                orgsDefaultFor.add(org);
            }
            
			public void add(Organization votes) {
				orgs.add(votes);
                if(!(disqualified && !MARK_NO_DISQUALIFY) && value!=null) {
                    score += votes.strength;
                }
			}
			public void vote(UserRegistry.User user) {
				voters.add(user);
			}

			public int compareTo(Object o) {
				if(o==this) { 
					return 0;
				}
				Chad other = (Chad)o;
								
				if(other.xpath == xpath) {
					return 0;
				}
				
				if(value == null) {
                    if(other.value == null) {
                        return 0;
                    } else {
                        return "".compareTo(other.value);
                    }
				} else {
                    if(other.value == null) {
                        return 1;
                    } else {
                        return value.compareTo(other.value);
                    }
				}
			}
		}

		// Race variables
		public int base_xpath;
		public String locale;
		public Hashtable<Integer,Chad> chads = new Hashtable<Integer,Chad>();
        public Hashtable<String,Chad> chadsByValue = new Hashtable<String,Chad>();
        public Hashtable<String,Chad> chadsByValue2 = new Hashtable<String,Chad>();
		public Hashtable<String, Organization> orgVotes = new Hashtable<String,Organization>();
		public Set<Chad> disputes = new TreeSet<Chad>();
		
		// winning info
		public Chad winner = null;
		public Status status = Status.INDETERMINATE;
		public Chad existing = null; // existing vote
        public Status existingStatus = Status.INDETERMINATE;
        int nexthighest = 0;
        public boolean hadDisqualifiedWinner = false; // at least one of the winners disqualified
        public boolean hadOtherError = false; // had an error on a missing item (coverage or collision?)
        int id; // for writing
        public Set<String> refConflicts=new HashSet<String>(); // had any items that differ only in refs?
            
		/* reset all */
		public void clear() {
			chads.clear();
            chadsByValue.clear();
            chadsByValue2.clear();
			orgVotes.clear();
			disputes.clear();
			winner = null;
            refConflicts.clear();
			existing=null;
			base_xpath = -1;
            nexthighest = 0;
            hadDisqualifiedWinner = false;
            hadOtherError = false;
		}
		
		/* Reset this for a new item */
		public void clear(int base_xpath, String locale) {
            clear(base_xpath, locale, -1);
		}

		/* Reset this for a new item */
		public void clear(int base_xpath, String locale, int id) {
			clear();
			this.base_xpath = base_xpath;
			this.locale = locale;
            this.id = id;
		}
		
		/**
		 * calculate the optimal item, if any
		 * recalculate any items
		 */
		public int optimal() throws SQLException {
			gatherVotes();
            
			calculateOrgVotes();
			
			Chad optimal = calculateWinner();
            
            if(!refConflicts.isEmpty()) {
                for(String conflictedValue : refConflicts) {
                    String findings = "";
                    if(conflictedValue==null) {
                        conflictedValue="";
                    }
                    int numNoRefs = 0;
                    int numRefs = 0;
                    int numNoRefsVotes = 0;
                    int numRefsVotes = 0;
                    int numVotes=0;
                    for(Chad c : chads.values()) {
                        if(!conflictedValue.equals(c.value)) {
                            continue;
                        }
                        if(c==optimal) {
                            findings = findings + "[winner]";
                        }
                        findings=findings+"#"+c.xpath;
                        int votes = c.voters.size();
                        if(c.refs!=null) {
                            findings = findings+","+"ref:"+c.refs;
                            numNoRefs++;
                            if(votes>0) {
                                numRefsVotes++;
                            }
                        } else {
                            numRefs++;
                            if(votes>0) {
                                numNoRefsVotes++;
                            }
                        }
                        if(c.score>0) {
                            findings = findings+",s"+c.score;
                        }
                        if(votes>0) {
                            findings = findings+",v"+votes;
                            numVotes++;
                        }
                        findings = findings +" ";
                    }
                    String conclusion="";
                    if(numVotes==0) {
                        conclusion="[No votes!]";
                    } else {
                        if(numVotes==1) {
                            if(numRefsVotes>0) {
                                conclusion="[Unanimous: REF]";
                            } else {
                                conclusion="[Unanimous: NO REF]";
                            }
                        } else if(numRefsVotes>1) {
                            conclusion="[Votes for differing references]";
                        } else if(numRefsVotes>0 && numNoRefsVotes>0) {
                            conclusion="[Votes for both REF and NONREF]";
                        } else {
                            // none of the above.
                            conclusion="[V"+numVotes+": r"+numRefsVotes+" no"+numNoRefsVotes+" - total: R"+numRefs+
                                        " NO"+numNoRefs+"]";
                        }
                    }
                        
                    System.err.println(locale+":"+base_xpath+" - refConflicts: "+conflictedValue+": " + conclusion +" :" +findings);
                }
            }
            
			if(optimal == null) {
				return -1;
			} else {
				return optimal.xpath;
			}
		}
        
        /* check for errors */
        boolean recountIfHadDisqualified() {
            boolean hadDisqualified = false;
            for(Chad c : chads.values()) {
                if(c.checkDisqualified()) {
                    hadDisqualified = true;
                }
            }
            if(winner == null) {
                /*if(test(locale, base_xpath, base_xpath, null)) { // check the base item - i.e. coverage... 
                    hadOtherError = true;
                }*/
                return hadDisqualified || hadOtherError;
            }
            if(!winner.disqualified) {
                return hadDisqualified;
            }
			
			if(!MARK_NO_DISQUALIFY) {  
				// actually remove winner, set to 0, etc.
				hadDisqualifiedWinner = true;
				winner = null; // no winner
				nexthighest = 0;
				calculateOrgVotes();

				calculateWinner();
			}
            return hadDisqualified;
        }
        
        private Chad getChad(int vote_xpath, int full_xpath, String value) {
            String valueForLookup = (value!=null)?value:EMPTY_STRING;
            String nonEmptyValue = valueForLookup;
            
            String full_xpath_string = sm.xpt.getById(full_xpath);
            String theReferences = null;
            if(full_xpath_string.indexOf(LDMLConstants.REFERENCES)>=0) {
                XPathParts xpp = new XPathParts(null,null);
                xpp.initialize(full_xpath_string);
                String lelement = xpp.getElement(-1);
                //String eAlt = xpp.findAttributeValue(lelement, LDMLConstants.ALT);
                theReferences = xpp.findAttributeValue(lelement,  LDMLConstants.REFERENCES);
                if(theReferences != null) {
                    // disambiguate it from the other value
                    valueForLookup = valueForLookup + " ["+theReferences+"]";
                    //if(value==null) {
                    //    value = "";
                    //}
                    //value = value + "&nbsp;<i title='This item has a Reference.'>(reference)</i>";
                }
            }

            Chad valueChad = chadsByValue.get(valueForLookup);
            if(valueChad != null) {
                return valueChad;
            } else {
                Chad otherChad = chadsByValue2.get(nonEmptyValue);
                if(otherChad!=null && otherChad.refs != theReferences) {
                    refConflicts.add(nonEmptyValue);
                }
            }
            
			Integer vote_xpath_int = new Integer(vote_xpath);
			Chad c = chads.get(vote_xpath_int);
			if(c == null) {
				c = new Chad(vote_xpath, full_xpath, value);
				chads.put(vote_xpath_int, c);
                chadsByValue.put(valueForLookup,c);
                chadsByValue2.put(nonEmptyValue,c);
                c.refs = theReferences;
			}
            return c;
        }
        
        private Organization getOrganization(String org) {
            Organization theirOrg = orgVotes.get(org);
            if(theirOrg == null) {
                theirOrg = new Organization(org);
                orgVotes.put(org,theirOrg);
            }
            return theirOrg;
        }

		private final void existingVote(int vote_xpath, int full_xpath, String value) {
			vote(null, vote_xpath, full_xpath, value);
		}
        
        private final void defaultVote(String org, int vote_xpath, int full_xpath, String value) {
            Chad c = getChad(vote_xpath, full_xpath, value);
            c.addDefault(getOrganization(org));
        }
		
		private void vote(UserRegistry.User user, int vote_xpath, int full_xpath, String value) {
			// add this vote to the chads, or create one if not present
            Chad c = getChad(vote_xpath, full_xpath, value);
			
			if(user != null) {
				c.vote(user);
				
				// add the chad to the set of orgs' chads
				Organization theirOrg = getOrganization(user.org);
                theirOrg.add(c);
			} else {
				// "existing" vote
				//Organization existingOrg = new Organization(c);
				//orgVotes.put(existingOrg.name, existingOrg);
				existing = c;
                
                if(vote_xpath == full_xpath && vote_xpath == base_xpath) { // shortcut: base=full means it is confirmed.
                    existingStatus = Status.APPROVED; 
                } else {
                    String fullpathstr = sm.xpt.getById(full_xpath);
                    //xpp.clear();
                    XPathParts xpp = new XPathParts(null,null);
                    xpp.initialize(fullpathstr);
                    String lelement = xpp.getElement(-1);
                    String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
                    if(eDraft==null || eDraft.equals("approved")) {
                        existingStatus = Status.APPROVED;
                    } else {
                        existingStatus = Status.UNCONFIRMED;
                    }
                }
			}
		}
		
		/**
		 * @returns number of votes counted, including abstentions
		 */ 
		private int gatherVotes() throws SQLException {
			queryVoteForBaseXpath.setString(1,locale);
            queryVoteForBaseXpath.setInt(2,base_xpath);

			queryValue.setString(1,locale);
			
            ResultSet rs = queryVoteForBaseXpath.executeQuery();
            int count =0;
            while(rs.next()) {
                count++;
                int submitter = rs.getInt(1);
                int vote_xpath = rs.getInt(2);
/*                String vote_value = rs.getString(4); */
                if(vote_xpath==-1) {
                    continue; // abstention
                }

				queryValue.setInt(2,vote_xpath);
				ResultSet crs = queryValue.executeQuery();
                int orig_xpath = vote_xpath;
				//String itemValue = "(could not find item#"+vote_xpath+")";
                String itemValue = null;
				if(crs.next()) {
					itemValue = crs.getString(1);
					orig_xpath = crs.getInt(2);
				}

                UserRegistry.User u = sm.reg.getInfo(submitter);
				vote(u, vote_xpath, orig_xpath, itemValue);
            }
			
            /**
             * Get the existing item. 
             */
			queryValue.setInt(2,base_xpath);
			rs = queryValue.executeQuery();
			if(rs.next()) {
				String itemValue = rs.getString(1);
				int origXpath = rs.getInt(2);
				existingVote(base_xpath, origXpath, itemValue);
			}
			
            // Check for default votes
            
            // Google: (proposed-x650)
            googData.setString(1,locale);
            googData.setInt(2,base_xpath);
            rs = googData.executeQuery(); // select xpath,origxpath,value from CLDR_DATA where alt_type='proposed-x650' and locale='af' and base_xpath=194130
            if(rs.next()) {
                int vote_xpath = rs.getInt(1);
                int origXpath = rs.getInt(2);
                String value = rs.getString(3);
                defaultVote("Google", vote_xpath, origXpath, value);
            }
            
            // Now, add ALL other possible items.
            dataByBase.setString(1,locale);
            dataByBase.setInt(2, base_xpath);
            rs = dataByBase.executeQuery();
            while(rs.next()) {
                int xpath = rs.getInt(1);
                int origXpath = rs.getInt(2);
                // 3 : alt_type
                String value = rs.getString(4);
                Chad c = getChad(xpath, origXpath, value);
            }
            
		    return count;
        }
		
		private void calculateOrgVotes() {
            for(Chad c : chads.values()) { // reset
                c.score = 0;
            }
			// calculate the org's choice
			for(Organization org : orgVotes.values()) {
				Chad vote = org.calculateVote();
				if(vote != null) {
					vote.add(org); // add that org's vote to the chad
				}
			}

            // look for any default votes. 
            
            for(Chad c : chads.values()) {
                if(c.disqualified && !MARK_NO_DISQUALIFY)  {
                    continue;
                }
                if(c.orgsDefaultFor != null) {
                    for(Organization o : c.orgsDefaultFor) {  // is this the default candidate vote for anyone?
                        if(o.votes.isEmpty()) {  // if they do not have any other votes..
                            int newStrength = getDefaultWeight(o.name, locale);
                            boolean suppressDefault = false; // any reason to suppress the default vote?
                            
                            if(newStrength == 1   // if it's only a vote of 1
                              && c.score == 0) {  // and no other organization voted for it
                                // See if anyone voted for anything else.
                                int otherStrengths = 0;
                                for(Chad k : chads.values()) {
                                    if(k == c) continue;  // votes for this item are OK 
                                    if(k.score>0) {
                                        suppressDefault = true;
//System.err.println("Suppressing DV on "+locale+":"+base_xpath+" because of other score.");
                                        break;
                                    }
                                }
                            } else {
                                int otherStrengths = 0;
                                for(Chad k : chads.values()) {
                                    if(k == c) continue;  // votes for this item are OK 
                                    if(k.score>0) {
//System.err.println("NOT Suppressing DV on "+locale+":"+base_xpath+" - other score, but newStrength="+newStrength+" and c.score="+c.score+".");
                                        break;
                                    }
                                }
                            }
                            if(!suppressDefault) {
                                // #2 pre-set the vote
//System.err.println("G voting "+newStrength+"  DV on "+locale+":"+base_xpath+" @:"+c.xpath+".");
                                o.setDefaultVote(c, newStrength);
                                // now, count it
                                Chad vote = o.calculateVote();
                                if(vote != null) {
                                    vote.add(o); // add that org's vote to the chad
                                }
                            } else {
                                orgVotes.remove(o.name); // remove the organization - it had no other votes.
                            }
                        }
                    }
                }
            }
		}
		
		private Chad calculateWinner() {
			int highest = 0;
			
			for(Chad c : chads.values()) {
                if((c.score==0) || (c.disqualified && !MARK_NO_DISQUALIFY)) continue;  // item had an error or is otherwise out of the running
                
				if(c.score > highest) { // new highest score
					disputes.clear();
					winner = c;
					nexthighest = highest; // save old nexthighest for 'N'
					highest = c.score;
				} else if(c.score == highest) { // same level
					nexthighest = highest; // save old nexthighest for 'N' ( == )
                    if(gOurCollator.compare(c.value, winner.value)<0) {
                        disputes.add(winner);  
                        winner = c; // new item (c) is lower in UCA, so use it
                    } else {
                        disputes.add(c);  // new item (c) was higher in UCA, add it to the disputes list
                    }
				} // else: losing item
			}
			
            // http://www.unicode.org/cldr/process.html#resolution_procedure
            if(highest > 0) {
                // Compare the optimal item vote O to the next highest vote getter N:
                if(highest >= (2*nexthighest) && highest >= 8) {      // O >= 2N && O >= 8
                    status = Status.APPROVED; // approved
                } else if(highest >= (2*nexthighest) && highest >= 2  // O>=2N && O>= 2 && G>= 2
                        && winner.orgs.size() >= 2) { 
                    status = Status.CONTRIBUTED;
                } else if(highest > nexthighest && highest >= 2) {    // O>N && O>=2
                    status = Status.PROVISIONAL;
                } else {
                    status = Status.UNCONFIRMED;
                }
            }
            
            // was there an approved item that wasn't replaced by a confirmed item?
            if( (existing != null) && (existingStatus == Status.APPROVED) &&  (!(existing.disqualified && !MARK_NO_DISQUALIFY)) && // good existing value
                ( (winner == null) || (status != Status.APPROVED))) // no new winner OR not-confirmed winner
            {
                if(winner != existing) {  // is it a different item entirely?
                    winner = existing;
                    nexthighest = highest; // record that the highest scoring was not the winner. "not enough votes to.."
                }
                status = Status.APPROVED; // mark it confirmed. ( == existingStatus )
            }
            
            // or, was there any existing item at all?
            if((winner == null) && (existing != null) && (!(existing.disqualified && !MARK_NO_DISQUALIFY))) {
                winner = existing;
                status = existingStatus; // whatever it is
            }
			
			return winner;
		}
		
		/**
		 * This function is called to update the CLDR_OUTPUT and CLDR_ORGDISPUTE tables.
		 * assumes a lock on (conn) held by caller.
         * @return type
		 */
		public int updateDB() throws SQLException {
			// First, zap old data
			int rowsUpdated = 0;
			
			// zap old CLDR_OUTPUT rows
			outputDelete.setString(1, locale);
			outputDelete.setInt(2, base_xpath);
			rowsUpdated += outputDelete.executeUpdate();
			
			orgDisputeDelete.setString(1, locale);
			orgDisputeDelete.setInt(2, base_xpath);
			rowsUpdated += orgDisputeDelete.executeUpdate();
			
			outputInsert.setString(1, locale);
			outputInsert.setInt(2, base_xpath);
			orgDisputeInsert.setString(2, locale);
			orgDisputeInsert.setInt(3, base_xpath);
			// Now, IF there was a valid result, store it.
			// 			outputInsert = prepareStatement("outputInsert", // loc, basex, outx, outFx, datax
			//   outputInsert:  #1 locale, #2 basex, #3 OUTx (the "user" xpath, i.e., no alt confirmed), #4 outFx #5 DATAx (where the data really lives.  For the eventual join that will find the data.)
			String baseString = sm.xpt.getById(base_xpath);

			XPathParts xpp = new XPathParts(null,null);
            xpp.clear();
            xpp.initialize(baseString);
            String lelement = xpp.getElement(-1);
            String eAlt = xpp.findAttributeValue(lelement, LDMLConstants.ALT);

			String alts[] = LDMLUtilities.parseAlt(eAlt);
			String altvariant = null;
			String baseNoAlt = baseString;
			
			if(alts[0] != null) { // it has an alt, so deconstruct it.
				altvariant = alts[0];
				baseNoAlt = sm.xpt.removeAlt(baseString);
			}
				
			if(winner != null) {
				int winnerPath = base_xpath; // shortcut - this IS the base xpath.
	
				String baseFString = sm.xpt.getById(winner.full_xpath);
				String baseFNoAlt = baseFString;
				if(alts[0]!=null) {
					baseFNoAlt = sm.xpt.removeAlt(baseFString);
				}
				
				baseFNoAlt = sm.xpt.removeAttribute(baseFNoAlt, "draft");
				baseFNoAlt = sm.xpt.removeAttribute(baseFNoAlt, "alt");

				int winnerFullPath = makeXpathId(baseFNoAlt, altvariant, null, status);
				
				if(winner.disqualified && MARK_NO_DISQUALIFY) {
					winnerFullPath = makeXpathId(baseFNoAlt, altvariant, "proposed-x555", status);
				}
			
				outputInsert.setInt(3, winnerPath); // outputxpath = base, i.e. no alt/proposed.
				outputInsert.setInt(4, winnerFullPath); // outputFullxpath = base, i.e. no alt/proposed.
				outputInsert.setInt(5, winner.xpath); // data = winner.xpath
                outputInsert.setInt(6, status.intValue());
				rowsUpdated += outputInsert.executeUpdate();
			}
		
            // add any other items
			int jsernum = 1000; // starting point for  x propoed designation
			for (Chad other : chads.values()) {
				// skip if:
				if( other == winner || (other.disqualified && !MARK_NO_DISQUALIFY)) {  // skip the winner and any disqualified items
                    continue;
				}
				jsernum++;
                Status proposedStatus = Status.UNCONFIRMED;
				String altproposed = "proposed-x"+jsernum;
				int aPath = makeXpathId(baseNoAlt,  altvariant, altproposed, Status.INDETERMINATE);
				
				String baseFString = sm.xpt.getById(other.full_xpath);
				String baseFNoAlt = baseFString;
				if(alts[0]!=null) {
					baseFNoAlt = sm.xpt.removeAlt(baseFString);
				}
				baseFNoAlt = sm.xpt.removeAttribute(baseFNoAlt, "draft");
				baseFNoAlt = sm.xpt.removeAttribute(baseFNoAlt, "alt");
				int aFullPath = makeXpathId(baseFNoAlt, altvariant, altproposed, proposedStatus);

				// otherwise, show it under something.
				outputInsert.setInt(3, aPath); // outputxpath = base, i.e. no alt/proposed.
				outputInsert.setInt(4, aFullPath); // outputxpath = base, i.e. no alt/proposed.
				outputInsert.setInt(5, other.xpath); // data = winner.xpath
                outputInsert.setInt(6, proposedStatus.intValue());
				rowsUpdated += outputInsert.executeUpdate();
			}
			
			// update disputes, if any
			for(Organization org : orgVotes.values()) {
				if(org.dispute) {
					orgDisputeInsert.setString(1, org.name);
					rowsUpdated += orgDisputeInsert.executeUpdate();
				}
			}

            // Now, update the vote results
            int resultXpath= -1;
            int type = 0;
            if(winner != null) {
                resultXpath = winner.xpath;
            }
            
            // Examine the results
            if(resultXpath != -1) {
                if(!disputes.isEmpty() &&
                        (status != Status.APPROVED)) { // if it was approved anyways, then it's not makred as disputed.
                    type = RES_DISPUTED;
                } else {
                    type = RES_GOOD;
                }
            } else {
                if(!chads.isEmpty()) {
                    type = RES_INSUFFICIENT;
                } else {
                    type = RES_NO_VOTES;
                }
            }
            if(hadDisqualifiedWinner || hadOtherError) {
                type = RES_ERROR;
            }
            if(id == -1) { 
                // Not an existing vote:  insert a new one
                // insert 
                insertResult.setInt(2,base_xpath);
                if(resultXpath != -1) {
                    insertResult.setInt(3,resultXpath);
                } else {
                    insertResult.setNull(3,java.sql.Types.SMALLINT); // no fallback.
                }
                insertResult.setInt(4,type);

                int res = insertResult.executeUpdate();
                // no commit
                if(res != 1) {
                    throw new RuntimeException(locale+":"+base_xpath+"=" + resultXpath+ " ("+typeToStr(type)+") - insert failed.");
                }
                id = -2; // unknown id
            } else {
                // existing vote: get the old one
                // ... for now, just do an update
                // update CLDR_RESULT set vote_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where id=?

                if(resultXpath != -1) {
                    updateResult.setInt(1,resultXpath);
                } else {
                    updateResult.setNull(1,java.sql.Types.SMALLINT); // no fallback.
                }
                updateResult.setInt(2, type);
                updateResult.setInt(3, base_xpath);
                updateResult.setString(4, locale);
                int res = updateResult.executeUpdate();
                if(res != 1) {
                    throw new RuntimeException(locale+":"+base_xpath+"@"+id+"="+resultXpath+ " ("+typeToStr(type)+") - update failed.");
                }
            }
			return type;
		}
	} // end Race
	
    Collator gOurCollator = createOurCollator();

    private static Collator createOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }
	
	public Race getRace(String locale, int base_xpath) throws SQLException {
		synchronized(conn) {
			// Step 0: gather all votes
			Race r = new Race();
			r.clear(base_xpath, locale);
			r.optimal();
			r.recountIfHadDisqualified();
			return r;
		}
	}
		

    /**
     * This function doesn't acquire a lock, so ONLY call it from updateResults().  Also, it does not call commmit().
     *
     * EXPECTS that the following is already called:
     *               dataByBase.setString(1,locale);
     *               queryVoteForBaseXpath.setString(1,locale);
     *               insertResult.setString(1,locale);
     * 
     *
     *
     * @param id ID of item to update (or, -1 if no existing item, i.e. needs to be inserted. 
     * @param locale the locale
     * @param base_xpath the base xpath that is being considered
     * @return the type of the vetting result. 
     */     
    private int updateResults(int id, String locale, int base_xpath, Set<Race> racesToUpdate) throws SQLException {
        int resultXpath = -1;
        int type = -1;
        int fallbackXpath = -1;

        boolean disputed = false; 
        queryValue.setString(1,locale);
        
        // Step 0: gather all votes
		Race r = new Race();
		r.clear(base_xpath, locale, id);
		resultXpath = r.optimal();
		
        racesToUpdate.add(r);
        
		// make the Race update itself ( we don't do this if it's just investigatory)
		type = r.updateDB();
        
        return type;
    }
    
    /**
     * fetch the result of vetting.
     * @param locale locale to look at
     * @param base_xpath the item under consideration
     * @param type an OUT parameter, must be a 1-element ( new int[1] ) array. input value doesn't matter. on output, 
     * the result type. 
     * @return the winning xpath id
     * @see updateResults
     */
    int queryResult(String locale, int base_xpath, int type[]) {
        // queryResult:    "select CLDR_RESULT.vote_xpath,CLDR_RESULT.type from CLDR_RESULT where (locale=?) AND (base_xpath=?)");
        synchronized(conn) {
            try {
                queryResult.setString(1, locale);
                queryResult.setInt(2, base_xpath);

                ResultSet rs = queryResult.executeQuery();
				int rv = -1;
				
                if(rs.next()) {
                    type[0] = rs.getInt(2);
                    rv = rs.getInt(1);
                    if(rv <= 0) {
                        rv = -1;
                    }
					rs.close();
                } else {
					type[0]=0;
					rv = -1;
				}
				rs.close();
                return rv;
            } catch ( SQLException se ) {
                type[0]=0; // doesn't matter here..
                String complaint = "Vetter:  couldn't query voting result for  " + locale + ":"+base_xpath+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
    }
    
    Status queryResultStatus(String locale, int base_xpath) {
        // queryResult:    "select CLDR_RESULT.vote_xpath,CLDR_RESULT.type from CLDR_RESULT where (locale=?) AND (base_xpath=?)");
        synchronized(conn) {
            try {
                outputQueryStatus.setString(1, locale);
                outputQueryStatus.setInt(2, base_xpath);

                ResultSet rs = outputQueryStatus.executeQuery();
				int rv = -1;
				
                if(rs.next()) {
                    rv = rs.getInt(1);
                    if(rv < 0) {
                        rv = -1;
                    }
					rs.close();
                } else {
					rv = -1;
				}
				rs.close();
                return Status.find(rv);
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't query outputQueryStatus for  " + locale + ":"+base_xpath+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
    }
	
    /**
     * What was the most privileged user who voted for this? 
     * i.e. if a Vetter and a TC vote for it, it returns TC.
     * @param locale locale
     * @param base_xpath the base xpath
     * @return UserRegistry.User level
     */
	int highestLevelVotedFor(String locale, int base_xpath) {
		int rv=-1;
        synchronized(conn) {
            try {
                highestVetter.setString(1, locale);
                highestVetter.setInt(2, base_xpath);

                ResultSet rs = highestVetter.executeQuery();
                if(rs.next()) {
                    rv = rs.getInt(1);
                }
				rs.close();
                return rv;
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't query voting highest for  " + locale + ":"+base_xpath+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
	}
    
    /**
     * Undo a vote. Whatever the vote was, remove it.
     * This is useful if we are about to recalculate the vote or recast it.
     * @param locale locale
     * @param base_xpath
     * @param submitter userid of the submitter
     * @return 1 if successful
     */
    int unvote(String locale, int base_xpath, int submitter) {
        //rmVote;
        synchronized(conn) {
            try {
                rmVote.setString(1,locale);
                rmVote.setInt(2,submitter);
                rmVote.setInt(3,base_xpath);
                
                int rs = rmVote.executeUpdate();
                conn.commit();
                return rs;
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't rm voting  for  " + locale + ":"+base_xpath+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
    }
    
    /**
     * Cast a vote.
     * @param locale the locale
     * @param base_xpath the base xpath
     * @param submitter the submitter's ID
     * @param vote_xpath the exact xpath voting for
     * @param type either VET_IMPLICIT or VET_EXPLICIT if this is to be recorded as an implicit or explicit vote.
     */
     
    void vote(String locale, int base_xpath, int submitter, int vote_xpath, int type) {
        synchronized(conn) {
            try {
                queryVoteId.setString(1,locale);
                queryVoteId.setInt(2,submitter);
                queryVoteId.setInt(3,base_xpath);
                
                ResultSet rs = queryVoteId.executeQuery();
                if(rs.next()) {
                    // existing
                    int id = rs.getInt(1);
                    updateVote.setInt(1, vote_xpath);
                    updateVote.setInt(2, type);
                    updateVote.setInt(3, id);
                    updateVote.executeUpdate();
//                    System.err.println("updated CLDR_VET #"+id);
                } else {
                    insertVote.setString(1,locale);
                    insertVote.setInt(2,submitter);
                    insertVote.setInt(3,base_xpath);
                    insertVote.setInt(4,vote_xpath);
                    insertVote.setInt(5,type);
                    insertVote.executeUpdate();
                }
      /*          rmResult.setString(1,locale);
                rmResult.setInt(2,base_xpath);
                rmResult.executeUpdate();
*/
              //  updateResults(locale);// caller needs to do updateResults
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't query voting result for  " + locale + ":"+base_xpath+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
    }
    
    /**
     *  called from updateStatus(). 
     * Collects the vetting type ( good, disputed, etc.. ) and updates the locale-level status row.
     * Assumes caller has lock, and that caller will call commit()
     * @param locale the locale
     * @param isUpdate true if to do an update, otherwise insert
     * @return number of rows affected, normally 1 if successful.
     */
    private int updateStatus(String locale, boolean isUpdate) throws SQLException {
        queryTypes.setString(1, locale);
        ResultSet rs = queryTypes.executeQuery();
        
        int t = 0;
        while(rs.next()) {
            t |= rs.getInt(1);
        }

        if(isUpdate==true) {
            updateStatus.setInt(1,t);
            updateStatus.setString(2,locale);
            return updateStatus.executeUpdate();
        } else {
            insertStatus.setInt(1,t);
            insertStatus.setString(2,locale);
            return insertStatus.executeUpdate();
        }
        
    }
    
    /**
     * Update any status which is missing. 
     * @return number of locales updated
     */
    public int updateStatus() { // updates MISSING status
        synchronized(conn) {
            // missing ones 
            int locs=0;
            int count=0;
            try {
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("select distinct CLDR_DATA.locale from CLDR_DATA where not exists ( select * from CLDR_STATUS where CLDR_STATUS.locale=CLDR_DATA.locale)");
                while(rs.next()) {
                    count += updateStatus(rs.getString(1), false);
                    locs++;
                }
                conn.commit();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  update status - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
            return count;
        }
    }
    
    /**
     * Get the status of a particular locale, as a bitwise OR of good, disputed, etc.. 
     * @param locale the locale
     * @return bitwise OR of good, disputed, etc.
     */
    int status(String locale) {
        synchronized(conn) {
            // missing ones 
            int locs=0;
            int count=0;
            try {
                queryStatus.setString(1,locale);
                ResultSet rs = queryStatus.executeQuery();
                if(rs.next()) {
                    int i = rs.getInt(1);
                    rs.close();
                    return i;
                } else {
                    rs.close();
                    return -1;
                }
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query status - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
    }
    
    
    /**
     * mailBucket:
     *   mail waiting to go out.
     * Hashmap:
     *    Integer(userid)   ->   String body-of-mail-to-send
     * 
     * This way, users only get one mail per service.
     * 
     * this function sends out mail waiting in the buckets.
     * 
     * @param mailBucket map of mail going out already. (IN)
     * @param title the title of this mail
     * @return number of mails sent.
     */
    int sendBucket(Map mailBucket, String title) {
        int n =0;
        String from = sm.survprops.getProperty("CLDR_FROM","nobody@example.com");
        String smtp = sm.survprops.getProperty("CLDR_SMTP",null);
//        System.err.println("FS: " + from + " | " + smtp);
        boolean noMail = (smtp==null);

///*srl*/		noMail = true;
		
        for(Iterator li = mailBucket.keySet().iterator();li.hasNext();) {
            Integer user = (Integer)li.next();
            String s = (String)mailBucket.get(user);            
            UserRegistry.User u = sm.reg.getInfo(user.intValue());
            
            if(!UserRegistry.userIsTC(u)) {
                s = "Note: If you have questions about this email,  instead of replying here,\n " +
                    "please contact your CLDR-TC representiative for your organization ("+u.org+").\n"+
                    "You can find the TC users listed near the top if you click '[List "+u.org+" Users] in the SurveyTool,\n" +
                    "Or, at http://www.unicode.org/cldr/apps/survey?do=list\n"+
                    "If you are unable to contact them, then you may reply to this email. Thank you.\n\n\n"+s;
            }
            
            
            if(!noMail) {
                MailSender.sendMail(smtp,null,null,from,u.email, title, s);
            } else {
                System.err.println("--------");
                System.err.println("- To  : " + u.email);
                System.err.println("- Subj: " + title);
                System.err.println("");
                System.err.println(s);
            }
            n++;
            if((n%50==0)) {
                System.err.println("Vetter.MailBucket: sent email " + n + "/"+mailBucket.size());
            }
        }
        return n;
    }
    
    /**
     * Send out the Nag emails.
     */
    int doNag() {
        //Map mailBucket = new HashMap(); // mail bucket: 
    
        Map intGroups = sm.getIntGroups();
        
        System.err.println("--- nag ---");
        int skipped=0;
        int mailed = 0;
        for(Iterator li = intGroups.keySet().iterator();li.hasNext();) {
            String group = (String)li.next();
           /* if(sm.isUnofficial && !group.equals("tlh") && !group.equals("und")) {
                skipped++;
                continue;
            }*/
            Set s = (Set)intGroups.get(group);
            mailed += doNag(group, s);
        }
        if((skipped>0)||(mailed>0)) {
            System.err.println("--- nag: skipped " + skipped +", mailed " + mailed);
        }
        return mailed;
    }
    
    /** 
     * compose nags for one group
     * @param intUsers interested users in this group
     * @param group the interest group being processed
     * @param s the list of locales contained in interest group 'group'
     */
    int doNag(String group, Set s) {
        // First, are there any problems here?
        String complain = null;
        int mailsent=0;
        boolean didPrint =false;
        
        Set<String> cc_emails = new HashSet<String>();
        Set<String> bcc_emails = new HashSet<String>();
        
        int emailCount = sm.fora.gatherInterestedUsers(group, cc_emails, bcc_emails);
        
        int genCountTotal=0;

        if(emailCount == 0) {
            return 0; // no interested users.
        }
        
        // for each locale in this interest group..
        for(Iterator li=s.iterator();li.hasNext();) {
            String loc = (String)li.next();
            
            int locStatus = status(loc);
            int genCount = sm.externalErrorCount(loc);
            genCountTotal += genCount;
            if((genCount>0) || (locStatus>=0 && (locStatus&RES_BAD_MASK)>1)) {
                int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
                int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
                int numDisputed = countResultsByType(loc,RES_DISPUTED);
                int numErrored = countResultsByType(loc,RES_ERROR);
                
                boolean localeIsDefaultContent = (null!=sm.supplemental.defaultContentToParent(loc));
                
                if(localeIsDefaultContent) {
                    //System.err.println(loc +" - default content, not sending notice. ");
                    continue;
                }
                
                if(numDisputed==0 && sm.isUnofficial) {
                    System.err.println("got one @ " + genCount + " -- " + loc);
                }
                
                if(complain == null) {
                    complain = "";
                }
/*
                if(complain == null) {
//                    System.err.println(" -nag: " + group);
                    complain = "\n\n* Group '" + group + "' ("+new ULocale(group).getDisplayName()+")  needs attention:  ";
                }
//                System.err.println("  -nag: " + loc + " - " + typeToStr(locStatus));
*/
/*
                String problem = "";
                if((numNoVotes+numInsufficient)>0) {
                    problem = problem + " INSUFFICIENT VOTES: "+(numNoVotes+numInsufficient)+" ";
                }
                if(numDisputed>0) {
                    problem = problem + " DISPUTED VOTES: "+numDisputed+"";
                }
                if(numErrored>0) {
                    problem = problem + " ERROR ITEMS: "+numErrored+"";
                }
                if(genCount>0) {
                    problem = problem + " ERROR ITEMS: "+numErrored+"";
                }
*/
                complain = complain +  new ULocale(loc).getDisplayName() + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc+"\n\n";
            }
        }
        
        if(complain != null) { // anything to send?
            String from = sm.survprops.getProperty("CLDR_FROM","nobody@example.com");
            String smtp = sm.survprops.getProperty("CLDR_SMTP",null);
            
            String disp = new ULocale(group).getDisplayName();
            
            String otherErr = "";
            if(genCountTotal>0) {
                otherErr = "\nAlso, there are  " +genCountTotal+" other errors for these locales listed at:\n    "+
                    sm.externalErrorUrl(group)+"\n\n";
            }
            
            String subject = "CLDR Vetting update: "+group + " (" + disp + ")";
            String body = "There are errors or disputes remaining in the locale data for "+disp+".\n"+
                "\n"+
                "Please go to http://www.unicode.org/cldr/vetting.html and follow the instructions to address the problems.\n"+
                "\n"+
//                "WARNING: there are some problems in computing the error and dispute counts, so please read that page even if you have read it before!\n" +
                "\n" + complain + "\n"+                otherErr+
                "Once you think that all the problems are addressed, forward this email message to surveytool@unicode.org, asking for your locale to be verified as done. We are working on a short time schedule, so we'd appreciate your resolving the issues as soon as possible. Remember that you will need to be logged-in before making changes.\n"+
                "\n\nThis is an automatic message, periodically generated to update vetters on the progress on their locales.\n\n";

            if(!bcc_emails.isEmpty()) {
                mailsent++;
                MailSender.sendBccMail(smtp, null, null, from, bcc_emails, subject, body);
            }
            if(!cc_emails.isEmpty()) {
                mailsent++;
                MailSender.sendToMail (smtp, null, null, from,  cc_emails, subject, body);
            }
            
        }
        return mailsent;
    }


    /**
     * Send out dispute nags for one organization.
     * @param  message your special message
     * @param org the organization
     * @return number of mails sent.
     */
    int doDisputeNag(String message, String org) {
if(true == true)    throw new InternalError("removed from use.");
        Map mailBucket = new HashMap(); // mail bucket: 
    
        Map intGroups = sm.getIntGroups();
        Map intUsers = sm.getIntUsers(intGroups);
        
        System.err.println("--- nag ---");
        
        for(Iterator li = intGroups.keySet().iterator();li.hasNext();) {
            String group = (String)li.next();
            Set s = (Set)intGroups.get(group);            
            doDisputeNag(mailBucket, (Set)intUsers.get(group), group, s, message, org);
        }
        
        if(mailBucket.isEmpty()) {
            System.err.println("--- nag: nothing to send.");
			return 0;
        } else {
            int n= sendBucket(mailBucket, "CLDR Dispute Report for " + ((org!=null)?org:" SurveyTool"));
            System.err.println("--- nag: " + n + " emails sent.");
			return n;
        }
    }


	/**
	 * Compose mail to certain users with details about disputed items.
	 *  
	 * @param mailBucket the bucket of outbound mail
	 * @param intUsers map of locale -> users
	 * @param group which group this locale is in
	 * @param message special message 
	 * @param users ONLY send mail to these users
	 */
    void doDisputeNag(Map mailBucket, Set intUsers, String group, Set locales, String message, String org) {
		//**NB: As this function was copied from doNag(), it will have some commented out parts from that function
		//**    for future features.

        // First, are there any problems here?
        String complain = null;
        
        if((intUsers==null) || intUsers.isEmpty()) {
            // if noone cares ...
            return;
        }
        
        boolean didPrint =false;
        for(Iterator li=locales.iterator();li.hasNext();) {
            String loc = (String)li.next();
            
            int locStatus = status(loc);
            if((locStatus&(RES_DISPUTED|RES_ERROR))>0) {  // RES_BAD_MASK
//                int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
//                int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
                int numDisputed = countResultsByType(loc,RES_DISPUTED);
                
                if(complain == null) {
//                    System.err.println(" -nag: " + group);
                    complain = "\n\n* Group '" + group + "' ("+new ULocale(group).getDisplayName()+")  needs attention:  DISPUTED VOTES: "+numDisputed+" \n";
                }
//                System.err.println("  -nag: " + loc + " - " + typeToStr(locStatus));
                String problem = "";
//                if((numNoVotes+numInsufficient)>0) {
//                    problem = problem + " INSUFFICIENT VOTES: "+(numNoVotes+numInsufficient)+" ";
//                }
                if(numDisputed>0) {
                    problem = problem + " DISPUTED VOTES: "+numDisputed+"\n\n";
                }

				// Get the actual XPaths
//				Hashtable insItems = new Hashtable();
				Hashtable disItems = new Hashtable();
				synchronized(this.conn) { 
					try { // moderately expensive.. since we are tying up vet's connection..
						ResultSet rs = this.listBadResults(loc);
						while(rs.next()) {
							int xp = rs.getInt(1);
							int type = rs.getInt(2);
							
							String path = sm.xpt.getById(xp);
							
							String theMenu = SurveyMain.xpathToMenu(path);
							
							if(theMenu != null) {
								if(type == Vetting.RES_DISPUTED) {
									disItems.put(theMenu, "");// what goes here?
								} /* else {
									insItems.put(theMenu, "");
								}*/ 
							}
						}
						rs.close();
					} catch (SQLException se) {
						throw new RuntimeException("SQL error listing bad results - " + SurveyMain.unchainSqlException(se));
					}
				}
				//WebContext subCtx = new WebContext(ctx);
				//subCtx.addQuery("_",ctx.locale.toString());
				//subCtx.removeQuery("x");

				if(numDisputed>0) {
					for(Iterator li2 = disItems.keySet().iterator();li2.hasNext();) {
						String item = (String)li2.next();
						
						complain = complain + "http://www.unicode.org/cldr/apps/survey?_="+loc+"&amp;x="+item.replaceAll(" ","+")+"&only=disputed\n";
					}
				}
                //complain = complain + "\n "+ new ULocale(loc).getDisplayName() + " - " + problem + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc;
            }
        }
        if(complain != null) {
            for(Iterator li = intUsers.iterator();li.hasNext();) {
                UserRegistry.User u = (UserRegistry.User)li.next();
				if((org != null) && (!u.org.equals(org))) {
					continue;
				}
//				if(!users.contains(u)) continue; /* TODO: optimize as a single boolean op */
                Integer intid = new Integer(u.id);
                String body = (String)mailBucket.get(intid);
                if(body == null) {
                    body = message + "\n\nYou will need to be logged-in before making changes at these URLs.\n\n";
                }
                body = body + complain + "\n";
                mailBucket.put(intid,body);
            }
        }
    }

    
    /**
     * how many results of this type for this locale? I.e. N disputed .. 
     * @param locale the locale
     * @param type the type to consider (not a bitfield)
     * @return number of results in this locale that are of this type
     */
    int countResultsByType(String locale, int type) {
        int rv = 0;
        synchronized(conn) {
            try {
                countResultByType.setString(1,locale);
                countResultByType.setInt(2, type);
                
                ResultSet rs = countResultByType.executeQuery();
                if(rs.next()) {
                    rv =  rs.getInt(1);
                }
                rs.close();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query count - loc=" + locale + ", type="+typeToStr(type)+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return rv;
    }
    
    /**
     * Pull in a ResultSet of the bad (unresolved) results for this locale
     * @param locale the locale
     * @return the ResultSet - caller must close it, etc!
     */
    ResultSet listBadResults(String locale) {
        ResultSet rs = null;
        synchronized(conn) {
            try {
                listBadResults.setString(1,locale);
                
                rs = listBadResults.executeQuery();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query bad results - loc=" + locale + ", type=BAD - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return rs;
    }
    
    /**
     * get the timestamp of when this group was last nagged
     * @param forNag TRUE for nag,  FALSE for informational
     * @param locale which locale
     * @param reset should the timestamp be reset?? [ TODO: currently this param is IGNORED. ]
     * @return the java.sql.Timestamp of when this group was last nagged.
     */
    java.sql.Timestamp getTimestamp(boolean forNag, String locale, boolean reset) {
        java.sql.Timestamp ts = null;
        synchronized(conn) {
            try {
                intQuery.setString(1,locale);
                
                ResultSet rs = intQuery.executeQuery();
                if(rs.next()) {
                    ts =  rs.getTimestamp(forNag?1:2);
                }
                rs.close();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query timestamp - nag=" + forNag + ", forRest="+reset+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return ts;
    }
    
    int getOrgDisputeCount(String org, String locale) {
        int count = 0;
        synchronized(conn) {
            try {
                orgDisputePathCount.setString(1,org);
                orgDisputePathCount.setString(2,locale);
                
                ResultSet rs = orgDisputePathCount.executeQuery();
                if(rs.next()) {
                    count =  rs.getInt(1);
                }
                rs.close();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query org dispute count " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return count;  
    }

    boolean queryOrgDispute(String org, String locale, int base_xpath) {
        boolean result = false;
        synchronized(conn) {
            try {
                orgDisputeQuery.setString(1,org);
                orgDisputeQuery.setString(2,locale);
                orgDisputeQuery.setInt(3,base_xpath);
                
                ResultSet rs = orgDisputeQuery.executeQuery();
                if(rs.next()) {
                    result =  true;
                }
                rs.close();
            } catch ( SQLException se ) {
                String complaint = "Vetter: couldn't  query org dispute count " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return result;  
    }
    
    void doOrgDisputePage(WebContext ctx) {
        if(ctx.session.user == null ||
            ctx.session.user.org == null) {
                return;
        }
        
        String loc = ctx.field("_");
        final String org = ctx.session.user.org;
        if(loc.equals("")) {
            synchronized(conn) {
                try {
                    orgDisputeLocs.setString(1,org);
                    
                    ResultSet rs = orgDisputeLocs.executeQuery();
                    if(rs.next()) {
                        ctx.println("<h4>Vetting Disputes for "+ctx.session.user.org+"</h4>");
                        
                        do {
                            loc = rs.getString(1);
                            int cnt = rs.getInt(2);
                            
                            sm.printLocaleLink(ctx,loc,new ULocale(loc).getDisplayName());
                            ctx.println(" "+cnt+" vetting disputes for " + org + "<br>");
                        } while(rs.next());
                    }
                    rs.close();
                } catch ( SQLException se ) {
                    String complaint = "Vetter:  couldn't  query orgdisputes " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    se.printStackTrace();
                    throw new RuntimeException(complaint);
                }
            }
        } else {
            // individual locale
        }
    }
	
    /**
     * Show the 'disputed' page.
     * @param ctx webcontext for IN/OUT stuff
     */
    void doDisputePage(WebContext ctx) {
        Map m = new TreeMap();
        WebContext subCtx = (WebContext)ctx.clone();
        subCtx.setQuery("do","");
        int n = 0;
        int locs=0;
        synchronized(conn) {
         try {
                // select CLDR_RESULT.locale,CLDR_XPATHS.xpath from CLDR_RESULT,CLDR_XPATHS where CLDR_RESULT.type=4 AND CLDR_RESULT.base_xpath=CLDR_XPATHS.id order by CLDR_RESULT.locale
            Statement s = conn.createStatement();
            ResultSet rs;
            
             if(ctx.hasField("only_err")) {
                ctx.println("<h1>Only showing ERROR (disqualified winner) items</h1>");
                rs = s.executeQuery("select CLDR_RESULT.locale,CLDR_RESULT.base_xpath from CLDR_RESULT where (CLDR_RESULT.type="+RES_ERROR+")");
             } else {
                rs = s.executeQuery("select CLDR_RESULT.locale,CLDR_RESULT.base_xpath from CLDR_RESULT where (CLDR_RESULT.type>"+RES_NO_VOTES+") AND (CLDR_RESULT.type<="+RES_BAD_MAX+")");
            }
			while(rs.next()) {
				n++;
				String aLoc = rs.getString(1);
				int aXpath = rs.getInt(2);
				String path = sm.xpt.getById(aXpath);
				String theMenu = SurveyMain.xpathToMenu(path);
                
				if(theMenu==null) {
					theMenu="raw";
				}
				Hashtable ht = (Hashtable)m.get(aLoc);
				if(ht==null) {
					locs++;
					ht = new Hashtable();
					m.put(aLoc,ht);
				}
				Set st = (Set)ht.get(theMenu);
				if(st==null) {
					st = new TreeSet();
					ht.put(theMenu,st);
				}
				st.add(path);
			}
			ctx.println("<hr>"+n+" disputed, error, or insufficient items total in " + m.size() + " locales.<br>");			
         } catch ( SQLException se ) {
            String complaint = "Vetter:  couldn't do DisputePage - " + SurveyMain.unchainSqlException(se);
            logger.severe(complaint);
            se.printStackTrace();
            throw new RuntimeException(complaint);
         }
        }

        boolean showAllXpaths = ctx.prefBool(sm.PREF_GROTTY);
        
        ctx.println("<table class='list'>");
        ctx.println("<tr class='botbar'>"+
//            "<th>Errs</th>" +
            "<th>#</th><th align='left' class='botgray'>Locale</th><th align='left'  class='botgray'>Disputed Sections</th></tr>");
        int nn=0;
        // todo: sort list..
        /*
        if(lm == null) {
            busted("Can't load CLDR data files from " + fileBase);
            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
        }

        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
        */
        TreeMap lm = sm.getLocaleListMap();
		for(Iterator i = lm.keySet().iterator();i.hasNext();) {
            String locName = (String)i.next();
            String loc = (String)lm.get(locName);

			Hashtable ht = (Hashtable)m.get(loc);

            // calculate the # of total disputed items in this locale
            int totalbad = 0;
            
            String groupName = new ULocale(loc).getLanguage();
            
            int genCount = 0;
            //genCount = sm.externalErrorCount(loc);
            
            if(ht != null) {
                for(Object o : ht.values()) {
                    Set subSet = (Set)o;
                    totalbad += subSet.size();
                }
            }
            
            if(totalbad==0 && genCount==0) continue;
            
			ctx.println("<tr class='row"+(nn++ % 2)+"'>");
        /*
            if(genCount>0) {
                ctx.print("<th align='left'><a href='"+sm.externalErrorUrl(groupName)+"'>"+genCount+"&nbsp;errs</a></th>");
            } else {
                ctx.print("<th></th>");
            }
        */
            ctx.print("<th align='left'>"+totalbad+"</th>");
            ctx.print("<th class='hang' align='left'>");
			sm.printLocaleLink(subCtx,loc,new ULocale(loc).getDisplayName().replaceAll("\\(",
                    "<br>(")); // subCtx = no 'do' portion, for now.
			ctx.println("</th>");
            if(totalbad > 0) {
                ctx.println("<td>");
                int jj=0;
                for(Iterator ii = ht.keySet().iterator();ii.hasNext();) {
                    if((jj++)>0) {
                        ctx.print(", ");
                    }
                    String theMenu = (String)ii.next();
                    Set subSet = (Set)ht.get(theMenu);
                    ctx.print("<a href='"+ctx.base()+"?"+
                        "_="+loc+"&amp;x="+theMenu+"&amp;only=disputed#"+DataSection.CHANGES_DISPUTED+"'>"+
                           theMenu.replaceAll(" ","\\&nbsp;")+"</a>&nbsp;("+ subSet.size()+")");
                        
                    if(showAllXpaths) {
                        ctx.print("<br><pre>");
                        for(Iterator iii = (subSet).iterator();iii.hasNext();) {
                            String xp = (String)iii.next();
                            ctx.println(xp);
                        }
                        ctx.print("</pre>");
                    }
                }
                ctx.print("</td>");
            }
            ctx.println("</tr>");
		}
        ctx.println("</table>");
	}
    
    /**
     * process all changes to this pod
     * @param podBase the XPATH base of a pod, see DataPod.xpathToPodBase
     * @param ctx WebContext of user
     * @return true if any changes were processed
     */
    public boolean processPodChanges(WebContext ctx, String podBase) {
        synchronized (ctx.session) {
            // first, do submissions.
            DataSection oldSection = ctx.getExistingSection(podBase);
            
            SurveyMain.UserLocaleStuff uf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
            CLDRFile cf = uf.cldrfile;
            if(cf == null) {
                throw new InternalError("CLDRFile is null!");
            }
            CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC) + " @ " + ctx.locale );
            }
            synchronized(ourSrc) { 
                // Set up checks
                CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make tests happen
            
                if(sm.processPeaChanges(ctx, oldSection, cf, ourSrc)) {
                    int j = sm.vet.updateResults(oldSection.locale); // bach 'em
                    ctx.println("<br> You submitted data or vote changes, and " + j + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
                    return true;
                }
            }
        }
        return false;
    }
    
    Hashtable<String, Reference<DataTester>> hash = new Hashtable<String, Reference<DataTester>>();

    public boolean test(String locale, int xpath, int fxpath, String value) {
        return test(locale, sm.xpt.getById(xpath), sm.xpt.getById(fxpath), value);
    }
    
    public boolean test(String locale, String xpath, String fxpath, String value) {
        DataTester tester = get(locale);
        return tester.test(xpath, fxpath, value);
    }

    private DataTester get(String locale) {
        Reference<DataTester> ref = hash.get(locale);
        DataTester d = null;
        if(ref != null) {
            d = ref.get();
        }
        if(d != null) {
            if(!d.isValid()) {
//                System.err.println("vetting::checker STALE " + locale);
                d.reset();
            }
        }
        if(d == null) {
            if(ref != null) {
//                System.err.println("vetting::checker EXPIRED " + locale);
            }
            d = new DataTester(locale);
            hash.put(locale, new SoftReference(d));
        }
        return d;
    }

    private class DataTester extends Registerable {

        //////
        
        CLDRFile file;
        CheckCLDR check;
        List overallResults = new ArrayList();
        List individualResults = new ArrayList();
        Map options = sm.basicOptionsMap();
        
        void reset() {
//            System.err.println("vetting::checker reset " + locale);
            CLDRDBSource dbSource = sm.makeDBSource(conn, null, new ULocale(locale), false);
            dbSource.vettingMode(sm.vet);
            //if(resolved == false) {
                file = sm.makeCLDRFile(dbSource);
            //} else { 
            //    file = new CLDRFile(dbSource,true);
            //}
            
            // [md] Set the coverage level to minimal and organization to none. That will override the organization and be consistent across all users.

            overallResults.clear();
            check = sm.createCheckWithoutCollisions();
            check.setCldrFileToCheck(file, options, overallResults);
            setValid();
            register();
        }
        
        private DataTester(String locale) 
        {
            super(sm.lcr, locale);

            options.put("CheckCoverage.requiredLevel","minimal");
            options.put("CoverageLevel.localeType","");

            reset();
        }
        //String f2 = sm.xpt.getById(85048);
        boolean test(String xpath, String fxpath, String value) {
            individualResults.clear();
            check.check(xpath, fxpath, value, options, individualResults);  // they get the full course
            if(!individualResults.isEmpty()) {
                for(Object o : individualResults) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus)o;
                    if(status.getType().equals(status.errorType)) {
                        //if(locale.equals("fr")) {
                      //  if(/*sm.isUnofficial &&*/ xpath.indexOf("ii")!=-1) {
                       // if(f2.equals(xpath)) {
//                            System.err.println("ER: "+xpath + " // " + fxpath + " // " + value + " - " + status.toString());
                      //  }
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
