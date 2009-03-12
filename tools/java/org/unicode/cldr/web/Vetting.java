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

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.web.CLDRDBSourceFactory.CLDRDBSource;
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

    static String EMPTY_STRING = "";
    private static java.util.logging.Logger logger;
    
    public static final String CLDR_RESULT = "cldr_result"; /** constant for table access **/
    public static final String CLDR_VET = "cldr_vet"; /** constant for table access **/
    public static final String CLDR_INTGROUP = "cldr_intgroup"; /** constant for table access **/
    public static final String CLDR_STATUS = "cldr_status"; /** constant for table access **/
    public static final String CLDR_OUTPUT = "cldr_output"; /** constant for table access **/
    public static final String CLDR_ORGDISPUTE = "cldr_orgdispute"; /** constant for table access **/
    public static final String CLDR_ALLPATHS = "cldr_allpaths"; /** constant for table access **/
    
    
    public static final int VET_EXPLICIT = 0; /** 0: vote was explicitly made by user **/
    public static final int VET_IMPLIED  = 1; /** 1: vote was implicitly made by user as a result of new data entry **/
    public static final int VET_ADMIN  = 2; /** 1: vote was made by an admin or tc **/

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
	static final boolean MARK_NO_DISQUALIFY = false; // mark items with errors, don't disqualify. Makes proposed-x555 items.
    
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
                s.execute("create index srl_vindex on cldr_vet (locale,vote_xpath)"); // speed up /vxml/ output
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
            if(false) System.out.println("EXPLAIN EXTENDED " + sql.replaceAll("\\?", "'?'")+";");
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
    PreparedStatement rmOutputLoc = null;
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
            "select distinct "+CLDRDBSourceFactory.CLDR_DATA+".submitter,"+CLDRDBSourceFactory.CLDR_DATA+".base_xpath from "+CLDRDBSourceFactory.CLDR_DATA+" WHERE "+
                "("+CLDRDBSourceFactory.CLDR_DATA+".submitter is not null)AND(locale=?) AND NOT EXISTS ( SELECT * from "+CLDR_VET+" where "+
                    "("+CLDRDBSourceFactory.CLDR_DATA+".locale="+CLDR_VET+".locale) AND ("+CLDRDBSourceFactory.CLDR_DATA+".base_xpath="+CLDR_VET+".base_xpath) AND "+
                    "("+CLDRDBSourceFactory.CLDR_DATA+".submitter="+CLDR_VET+".submitter) )");
            dataByUserAndBase = prepareStatement("dataByUserAndBase",
                "select "+CLDRDBSourceFactory.CLDR_DATA+".XPATH,"+CLDRDBSourceFactory.CLDR_DATA+".ALT_TYPE from "+CLDRDBSourceFactory.CLDR_DATA+" where submitter=? AND base_xpath=? AND locale=?");
            dataByBase = prepareStatement("dataByBase", /*  1:locale, 2:base_xpath  ->  stuff */
                "select xpath,origxpath,alt_type,value FROM "+CLDRDBSourceFactory.CLDR_DATA+" WHERE " + 
                    "(locale=?) AND (base_xpath=?)");
            insertVote = prepareStatement("insertVote",
                "insert into "+CLDR_VET+" (locale,submitter,base_xpath,vote_xpath,type,modtime) values (?,?,?,?,?,CURRENT_TIMESTAMP)");
            queryVote = prepareStatement("queryVote",
                "select "+CLDR_VET+".vote_xpath, "+CLDR_VET+".type from "+CLDR_VET+" where "+CLDR_VET+".locale=? AND "+CLDR_VET+".submitter=? AND "+CLDR_VET+".base_xpath=?");
            rmVote = prepareStatement("rmVote",
                "delete from "+CLDR_VET+" where "+CLDR_VET+".locale=? AND "+CLDR_VET+".submitter=? AND "+CLDR_VET+".base_xpath=?");
            queryVoteId = prepareStatement("queryVoteId",
                "select "+CLDR_VET+".id from "+CLDR_VET+" where "+CLDR_VET+".locale=? AND "+CLDR_VET+".submitter=? AND "+CLDR_VET+".base_xpath=?");
            updateVote = prepareStatement("updateVote",
                "update "+CLDR_VET+" set vote_xpath=?, type=?, modtime=CURRENT_TIMESTAMP where id=?");
            queryVoteForXpath = prepareStatement("queryVoteForXpath",
                "select "+CLDR_VET+".submitter from "+CLDR_VET+" where "+CLDR_VET+".locale=? AND "+CLDR_VET+".vote_xpath=?");
            queryVoteForBaseXpath = prepareStatement("queryVoteForBaseXpath",
                "select "+CLDR_VET+".submitter,"+CLDR_VET+".vote_xpath from "+CLDR_VET+" where "+CLDR_VET+".locale=? AND "+CLDR_VET+".base_xpath=?");
            missingResults = prepareStatement("missingResults", /*  1:locale  ->  1: base_xpath */
                "select distinct "+CLDRDBSourceFactory.CLDR_DATA+".base_xpath from "+CLDRDBSourceFactory.CLDR_DATA+" WHERE (locale=?) AND NOT EXISTS ( SELECT * from "+CLDR_RESULT+" where ("+CLDRDBSourceFactory.CLDR_DATA+".locale="+CLDR_RESULT+".locale) AND ("+CLDRDBSourceFactory.CLDR_DATA+".base_xpath="+CLDR_RESULT+".base_xpath)  )");
            missingResults2 = prepareStatement("missingResults2", /*  1:locale  ->  1: base_xpath */
                "select distinct "+CLDR_ALLPATHS+".base_xpath from "+CLDR_ALLPATHS+" WHERE NOT EXISTS ( SELECT * from "+CLDR_RESULT+" where ("+CLDR_RESULT+".locale=?) AND ("+CLDR_ALLPATHS+".base_xpath="+CLDR_RESULT+".base_xpath)  )");
            insertResult = prepareStatement("insertResult", 
                "insert into "+CLDR_RESULT+" (locale,base_xpath,result_xpath,type,modtime) values (?,?,?,?,CURRENT_TIMESTAMP)");
            rmResult = prepareStatement("rmResult", 
                "delete from "+CLDR_RESULT+" where (locale=?)AND(base_xpath=?)");
            rmResultAll = prepareStatement("rmResultAll", 
                "delete from "+CLDR_RESULT+"");
            rmResultLoc = prepareStatement("rmResultLoc", 
                "delete from "+CLDR_RESULT+" where locale=?");
            rmOutputLoc = prepareStatement("rmOutputLoc", 
                    "delete from "+CLDR_OUTPUT+" where locale=?");
            queryResult = prepareStatement("queryResult",
                "select "+CLDR_RESULT+".result_xpath,"+CLDR_RESULT+".type from "+CLDR_RESULT+" where (locale=?) AND (base_xpath=?)");
            countResultByType = prepareStatement("countResultByType",
                "select COUNT(base_xpath) from "+CLDR_RESULT+" where locale=? AND type=?");
            listBadResults = prepareStatement("listBadResults",
                "select base_xpath,type from "+CLDR_RESULT+" where locale=? AND type<="+RES_BAD_MAX);
            queryTypes = prepareStatement("queryTypes",
                "select distinct "+CLDR_RESULT+".type from "+CLDR_RESULT+" where locale=?");
            insertStatus = prepareStatement("insertStatus",
                "insert into "+CLDR_STATUS+" (type,locale,modtime) values (?,?,CURRENT_TIMESTAMP)");
            updateStatus = prepareStatement("updateStatus",
                "update "+CLDR_STATUS+" set type=?,modtime=CURRENT_TIMESTAMP where locale=?");
            queryStatus = prepareStatement("queryStatus",
                "select type from "+CLDR_STATUS+" where locale=?");
            staleResult = prepareStatement("staleResult",
                "select "+CLDR_RESULT+".id,"+CLDR_RESULT+".base_xpath from "+CLDR_RESULT+" where "+CLDR_RESULT+".locale=? AND exists (select * from "+CLDR_VET+" where ("+CLDR_VET+".base_xpath="+CLDR_RESULT+".base_xpath) AND ("+CLDR_VET+".locale="+CLDR_RESULT+".locale) AND ("+CLDR_VET+".modtime>"+CLDR_RESULT+".modtime))");
            updateResult = prepareStatement("updateResult",
                "update "+CLDR_RESULT+" set result_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where base_xpath=? and locale=?");
            intQuery = prepareStatement("intQuery",
                "select last_sent_nag,last_sent_update from CLDR_INTGROUP where intgroup=?");
            intAdd = prepareStatement("intAdd",
                "insert into "+CLDR_INTGROUP+" (intgroup) values (?)");
            intUpdateNag = prepareStatement("intUpdateNag",
                "update "+CLDR_INTGROUP+"  set last_sent_nag=CURRENT_TIMESTAMP where intgroup=?");
            intUpdateUpdate = prepareStatement("intUpdateUpdate",
                "update "+CLDR_INTGROUP+"  set last_sent_update=CURRENT_TIMESTAMP where intgroup=?");
            queryValue = prepareStatement("queryValue",
                "SELECT value,origxpath FROM " + CLDRDBSourceFactory.CLDR_DATA +
            " WHERE locale=? AND xpath=?");
            highestVetter = prepareStatement("highestVetter",
                    "select "+UserRegistry.CLDR_USERS+".userlevel from "+CLDR_VET+","+UserRegistry.CLDR_USERS+" where "+UserRegistry.CLDR_USERS+".id="+CLDR_VET+".submitter AND "+CLDR_VET+".locale=? AND "+CLDR_VET+".vote_xpath=? ORDER BY "+UserRegistry.CLDR_USERS+".userlevel");

            // CLDR_OUTPUT
            outputDelete = prepareStatement("outputDelete", // loc, basex
                    "delete from "+CLDR_OUTPUT+" where locale=? AND base_xpath=?");
            outputInsert = prepareStatement("outputInsert", // loc, basex, outx, outFx, datax
                    "insert into "+CLDR_OUTPUT+" (locale, base_xpath, output_xpath, output_full_xpath, data_xpath, status) values (?,?,?,?,?,?)");
            outputQueryStatus = prepareStatement("outputQueryStatus", // loc, basex, outx, outFx, datax
                    "select status from "+CLDR_OUTPUT+" where locale=? AND output_xpath=?");

            // CLDR_ORGDISPUTE
            orgDisputeDelete = prepareStatement("orgDisputeDelete", // loc, basex
                    "delete from "+CLDR_ORGDISPUTE+" where locale=? AND base_xpath=?");
            orgDisputeInsert = prepareStatement("orgDisputeInsert", // org, locale, base_xpath
                    "insert into "+CLDR_ORGDISPUTE+" (org, locale, base_xpath) values (?,?,?)");

            orgDisputeLocs = prepareStatement("orgDisputeLocs", 
                "select locale,count(*) from "+CLDR_ORGDISPUTE+" where org=? group by locale  order by locale");
            orgDisputePaths = prepareStatement("orgDisputePaths", 
                "select base_xpath from "+CLDR_ORGDISPUTE+" where org=? AND locale=?");
            orgDisputePathCount = prepareStatement("orgDisputePathCount", 
                "select COUNT(base_xpath) from "+CLDR_ORGDISPUTE+" where org=? AND locale=?");
            orgDisputeQuery = prepareStatement("orgDisputeQuery", 
                "select * from "+CLDR_ORGDISPUTE+" where org=? AND locale=? and base_xpath=?");
                
            googData = prepareStatement("googData",
                "select xpath,origxpath,value from "+CLDRDBSourceFactory.CLDR_DATA+" where alt_type='proposed-x650' and locale=? and base_xpath=?");
                
            allpathsAdd = prepareStatement("allpathsAdd",
                "insert into "+CLDR_ALLPATHS+" (base_xpath) values (?)");
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
            int count = updateImpliedVotes(CLDRLocale.getInstance(localeName));
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
    public int updateImpliedVotes(CLDRLocale locale) {
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
                dataByUserAndBase.setString(3, locale.toString());
                missingImpliedVotes.setString(1,locale.toString());
                insertVote.setString(1,locale.toString());
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
     * return the result of a particular user's vote
     * @param locale which locale
     * @param userid of the submitter
     * @param base_xpath base xpath id to check
     * @return result of vetting, RES_NO_VOTES, etc.
     * @see XPathTable
     */
    public int queryVote(CLDRLocale locale, int submitter, int base_xpath) {
    	return queryVote(locale, submitter, base_xpath, null);
    }

    /**
     * return the result of a particular user's vote
     * @param locale which locale
     * @param userid of the submitter
     * @param base_xpath base xpath id to check
     * @param type if non-null, item [0] receives the voting type
     * @return result of vetting, RES_NO_VOTES, etc.
     * @see XPathTable
     */
     public int queryVote(CLDRLocale locale, int submitter, int base_xpath, int type[]) {
        synchronized(conn) {
            try {
                queryVote.setString(1, locale.toString());
                queryVote.setInt(2, submitter);
                queryVote.setInt(3, base_xpath);
                ResultSet rs = queryVote.executeQuery();
                while(rs.next()) {
                    int vote_xpath = rs.getInt(1);
                    if(type != null) {
                    	type[0] = rs.getInt(2);
                    }
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
     * @param locale. which locale
     * @param base_xpath string xpath to gather for
     * @return a Set of UserRegistry.User objects
     */
     
    public Set<UserRegistry.User> gatherVotes(CLDRLocale locale, String base_xpath) {
        int base_xpath_id = sm.xpt.getByXpath(base_xpath);
        synchronized(conn) {
            try {
                queryVoteForXpath.setString(1, locale.toString());
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
                String localeString= fileName.substring(0,dot);
                CLDRLocale localeName = CLDRLocale.getInstance(localeString);
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
    public int updateResults(CLDRLocale locale) {
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
                    String complaint = "Vetter:  couldn't update "+CLDR_ALLPATHS+"" + " - " + SurveyMain.unchainSqlException(se);
                    logger.severe(complaint);
                    se.printStackTrace();
                    throw new RuntimeException(complaint);
                }
            }
            System.err.println("importantXpaths: calculated in " + et2 + " - " + gAllImportantXpaths.size() + ", "+updates+" updates");
        }
        return gAllImportantXpaths;
    }
    public int updateResults(CLDRLocale locale, boolean removeFirst) {
        int tcount=0;
        int lcount = 0;
        int types[] = new int[1];
        
        ElapsedTimer et2 = new ElapsedTimer();
        types[0]=0;
        
        int count = updateResults(locale,types, removeFirst);
        tcount += count;
        if(count>0) {
            lcount++;
            System.err.println("updateResults(1) ("+locale+ " ("+count+" updated, "+typeToStr(types[0])+") - "+1+"/"+1+") took " + et2.toString());
        } else {
            // no reason to print it.
        }
        return tcount;
    }
    
    public int updateResults(CLDRLocale locale, int type[]) {
        return updateResults(locale, type, false);
    }
    
    /**
     * update the results of a specific locale, and return the bitwise OR of all types of results had
     * @param locale which locale
     * @param type an OUT parameter, must be a 1-element ( new int[1] ) array. input value doesn't matter. on output, 
     * contains the bitwise OR of all types of vetting results which were found.
     * @return number of results changed
     **/
    public int updateResults(CLDRLocale locale, int type[], boolean removeFirst) {
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
                    rmResultLoc.setString(1, locale.toString());
                    int del = rmResultLoc.executeUpdate();
                    rmOutputLoc.setString(1, locale.toString());
                    int del2 = rmOutputLoc.executeUpdate();
                    System.err.println("** "+ locale + " - "+del+" results and "+del2+" outputs removed");
                    conn.commit();
                }            
                
                //dataByUserAndBase.setString(3, locale);
                missingResults.setString(1,locale.toString());
                //insertVote.setString(1,locale);
                //insertVote.setInt(5,VET_IMPLIED);
                
                // for updateResults(id, ...)
                dataByBase.setString(1,locale.toString());
                queryVoteForBaseXpath.setString(1,locale.toString());
                insertResult.setString(1,locale.toString());
                
                // Missing results...
//                ElapsedTimer et_m = new ElapsedTimer();
                ResultSet rs = missingResults.executeQuery();
//                System.err.println("Missing results for " + locale + " found in " + et_m.toString());
                while(rs.next()) {
                    ncount++;
                    base_xpath = rs.getInt(1);
                    
                    //todoPaths.remove(new Integer(base_xpath));
                    int rc = updateResults(-1, locale, base_xpath, racesToUpdate);

                    updates |= rc;
                }
//                System.err.println("Missing results "+ncount+" for " + locale + " updated in " + et_m.toString());
                // out of date results..
                if(false) {  // stale results should not be needed anymore
                    staleResult.setString(1,locale.toString());
                    rs = staleResult.executeQuery();  // id, base_xpath
//                    System.err.println("Stale results for " + locale + " found in " + et_m.toString());
                    while(rs.next()) {
                        ucount++;
                        int id = rs.getInt(1);
                        base_xpath = rs.getInt(2);
                        
                        int rc = updateResults(id, locale, base_xpath, racesToUpdate);
                        //    System.err.println("*Updated id " + id + " of " + locale+":"+base_xpath);
                        updates |= rc;
                    }
//                    System.err.println("Stale results "+ucount+" for " + locale + " updated in " + et_m.toString());
                }
                int mcount=0;

                // if anything changed, commit it
                if((ucount > 0) || (ncount > 0) || (mcount > 0)) {
                    int uscnt = updateStatus(locale, true);// update results
                    String uncount = " "+ncount+" missing, " + ucount+" stale ";
                    if(uscnt>0) {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to vote change - "+uncount);
                    } else {
                        System.err.println(locale+": updated " + uscnt + " statuses, due to vote change??"+uncount);
                    }
                    conn.commit();
                }
//                System.err.println("Committed or not for " + locale + " after " + et_m.toString());
                
                
                
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
    
//    private Set<Race> searchForErrors(Set<Race> racesToUpdate) {
////        System.err.println(racesToUpdate.size() + " to check");
//        Set<Race> errorRaces = new HashSet<Race>();
//        
//        for(Race r : racesToUpdate) {
//            if(r.recountIfHadDisqualified()) {
////if(r.base_xpath==85942)                System.err.println("Had errs; " + r.locale + " / " + r.base_xpath);
//                errorRaces.add(r);
//            }
//        }
//        
//        return errorRaces;
//    }
    
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
            int count = washVotes(CLDRLocale.getInstance(localeName)/*,types*/);
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
    public int washVotes(CLDRLocale locale /*, int type[]*/) {
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
        
        if(SurveyMain.db_Mysql) {
            throw new InternalError("Not implemented for mysql");
        }
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
                        "select xpath,value,source,origxpath from "+CLDRDBSourceFactory.CLDR_DATA+" where base_xpath=? AND SUBMITTER is NULL AND locale=?");
                }
                
                lookupByXpath.setString(2,locale.toString());
                int cachedBase = -1;
                int vettedValue = -1; // do we have a vetted value?
                Hashtable cachedProps = new Hashtable();

                try {
                    CLDRLocale ulocale = (locale);
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

                int vetCleared = s3.executeUpdate("delete from "+CLDR_VET+" where locale='"+locale+"'");
                int resCleared = s3.executeUpdate("delete from "+CLDR_RESULT+" where locale='"+locale+"'");
                
                // clear RESULTS
                System.err.println(locale + " - cleared "+vetCleared + " from CLDR_VET");
                System.err.println(locale + " - cleared "+resCleared + " from CLDR_RESULT");
                
                ResultSet rs = s.executeQuery("select id,source,value,base_xpath,submitter from "+CLDRDBSourceFactory.CLDR_DATA+" where SUBMITTER IS NOT NULL AND LOCALE='"+locale+"' order by base_xpath, source desc");
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
                        ncount += s3.executeUpdate("delete from "+CLDRDBSourceFactory.CLDR_DATA+" where id="+oldId);
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
        
		public static Status toStatus(VoteResolver.Status vrs) {
		    //missing, X unconfirmed, provisional, contributed, approved;
		    if(vrs==VoteResolver.Status.missing) {
		        return INDETERMINATE;
		    } else {
		        return valueOf(vrs.name().toUpperCase());
		    }
		}
		
		public VoteResolver.Status toVRStatus() {
		    if(this==INDETERMINATE) {
		        return VoteResolver.Status.missing;
		    } else {
		        return VoteResolver.Status.valueOf(name().toLowerCase());
		    }
		}
		
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
     
    int getDefaultWeight(String name, String locale) {
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

    Collator gOurCollator = createOurCollator();

    private static Collator createOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }
	
	public Race getRace(CLDRLocale locale, int base_xpath) throws SQLException {
		synchronized(conn) {
			// Step 0: gather all votes
			Race r = new Race(this);
			r.clear(base_xpath, locale);
			r.optimal();
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
    private int updateResults(int id, CLDRLocale locale, int base_xpath, Set<Race> racesToUpdate) throws SQLException {
        int resultXpath = -1;
        int type = -1;
        int fallbackXpath = -1;

        boolean disputed = false; 
        queryValue.setString(1,locale.toString());
        
        // Step 0: gather all votes
		Race r = new Race(this);
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
    int queryResult(CLDRLocale locale, int base_xpath, int type[]) {
        return getCachedLocaleData(locale).getWinningXPath(base_xpath, type);
    }
    
    int queryResultInternal(CLDRLocale locale, int base_xpath, int type[]) {
        // queryResult:    "select CLDR_RESULT.vote_xpath,CLDR_RESULT.type from "+CLDR_RESULT+" where (locale=?) AND (base_xpath=?)");
        synchronized(conn) {
            try {
                queryResult.setString(1, locale.toString());
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
    
    Status queryResultStatus(CLDRLocale locale, int base_xpath) {
        // queryResult:    "select CLDR_RESULT.vote_xpath,CLDR_RESULT.type from "+CLDR_RESULT+" where (locale=?) AND (base_xpath=?)");
        synchronized(conn) {
            try {
                outputQueryStatus.setString(1, locale.toString());
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
     * @return >0 if successful
     */
    int unvote(CLDRLocale locale, int base_xpath, int submitter) {
        //rmVote;
        synchronized(conn) {
            try {
                rmVote.setString(1,locale.toString());
                rmVote.setInt(2,submitter);
                rmVote.setInt(3,base_xpath);
                
                int rs = rmVote.executeUpdate();
                rmResult.setString(1,locale.toString());
                rmResult.setInt(2,base_xpath);
                rs += rmResult.executeUpdate();
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
     
    void vote(CLDRLocale locale, int base_xpath, int submitter, int vote_xpath, int type) {
        synchronized(conn) {
            try {
                queryVoteId.setString(1,locale.toString());
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
                    insertVote.setString(1,locale.toString());
                    insertVote.setInt(2,submitter);
                    insertVote.setInt(3,base_xpath);
                    insertVote.setInt(4,vote_xpath);
                    insertVote.setInt(5,type);
                    insertVote.executeUpdate();
                }
                rmResult.setString(1,locale.toString());
                rmResult.setInt(2,base_xpath);
                rmResult.executeUpdate();

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
    private int updateStatus(CLDRLocale locale, boolean isUpdate) throws SQLException {
        queryTypes.setString(1, locale.toString());
        ResultSet rs = queryTypes.executeQuery();
        
        int t = 0;
        while(rs.next()) {
            t |= rs.getInt(1);
        }

        if(isUpdate==true) {
            updateStatus.setInt(1,t);
            updateStatus.setString(2,locale.toString());
            return updateStatus.executeUpdate();
        } else {
            insertStatus.setInt(1,t);
            insertStatus.setString(2,locale.toString());
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
                ResultSet rs = s.executeQuery("select distinct "+CLDRDBSourceFactory.CLDR_DATA+".locale from "+CLDRDBSourceFactory.CLDR_DATA+" where not exists ( select * from "+CLDR_STATUS+" where "+CLDR_STATUS+".locale="+CLDRDBSourceFactory.CLDR_DATA+".locale)");
                while(rs.next()) {
                    count += updateStatus(CLDRLocale.getInstance(rs.getString(1)), false);
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
    int status(CLDRLocale locale) {
        synchronized(conn) {
            return getCachedLocaleData(locale).getStatus();
        }
    }
    public int getWinningXPath(int xpath, CLDRLocale locale) {
        return getCachedLocaleData(locale).getWinningXPath(xpath, null);
    }
    private int handleStatus(CLDRLocale locale) {
        synchronized(conn) {
            // missing ones 
            int locs=0;
            int count=0;
            try {
                queryStatus.setString(1,locale.toString());
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
    int doNag(String group, Set<CLDRLocale> s) {
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
        for(Iterator<CLDRLocale> li=s.iterator();li.hasNext();) {
            CLDRLocale loc = li.next();
            
            int locStatus = status(loc);
            int genCount = sm.externalErrorCount(loc);
            genCountTotal += genCount;
            if((genCount>0) || (locStatus>=0 && (locStatus&RES_BAD_MASK)>1)) {
                int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
                int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
                int numDisputed = countResultsByType(loc,RES_DISPUTED);
                int numErrored = countResultsByType(loc,RES_ERROR);
                
                boolean localeIsDefaultContent = (null!=sm.supplemental.defaultContentToParent(loc.toString()));
                
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
                complain = complain +  loc.toULocale().getDisplayName() + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc+"\n\n";
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
            CLDRLocale loc = CLDRLocale.getInstance(li.next().toString());
            
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
							
							String theMenu = PathUtilities.xpathToMenu(path);
							
							if(theMenu != null) {
								if(type == Vetting.RES_DISPUTED) {
									disItems.put(theMenu, "");
									
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
     * @param loc the locale
     * @param type the type to consider (not a bitfield)
     * @return number of results in this locale that are of this type
     */
    int countResultsByType(CLDRLocale loc, int type) {
        int rv = 0;
        synchronized(conn) {
            try {
                countResultByType.setString(1,loc.toString());
                countResultByType.setInt(2, type);
                
                ResultSet rs = countResultByType.executeQuery();
                if(rs.next()) {
                    rv =  rs.getInt(1);
                }
                rs.close();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query count - loc=" + loc + ", type="+typeToStr(type)+" - " + SurveyMain.unchainSqlException(se);
                logger.severe(complaint);
                se.printStackTrace();
                throw new RuntimeException(complaint);
            }
        }
        return rv;
    }
    
    /**
     * Pull in a ResultSet of the bad (unresolved) results for this locale
     * @param loc the locale
     * @return the ResultSet - caller must close it, etc!
     */
    ResultSet listBadResults(CLDRLocale loc) {
        ResultSet rs = null;
        synchronized(conn) {
            try {
                listBadResults.setString(1,loc.toString());
                
                rs = listBadResults.executeQuery();
            } catch ( SQLException se ) {
                String complaint = "Vetter:  couldn't  query bad results - loc=" + loc + ", type=BAD - " + SurveyMain.unchainSqlException(se);
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
    private static class WinType {
        public int win;
        public int type;
    }

    private class CachedVettingData extends Registerable {
        Integer status = null;
        public CachedVettingData(CLDRLocale locale) {
            super(sm.lcr, locale);
            register();
        }
        IntHash<WinType> winningXpathCache = null;

        public int getWinningXPath(int xpath, int[] type) {
            if(winningXpathCache==null) {
                winningXpathCache=new IntHash<WinType>();
            }
            WinType winning = winningXpathCache.get(xpath);
            if(winning==null) {
                if(type==null) type = new int[1];
                int winner = sm.vet.queryResultInternal(locale, xpath, type);
                winning = new WinType();
                winning.win=winner;;
                winning.type = type[0];
                winningXpathCache.put(xpath, winning);
            }
            return winning.win;
        }

        public int getStatus() {
            if(status==null) {
                status = handleStatus(locale);
            }
            return status;
        }

        private Map<String,Integer> disputeMap = new HashMap<String,Integer>();
        public int getOrgDisputeCount(String org) {
            Integer dc = disputeMap.get(org);
            if(dc == null) {
                dc = handleGetOrgDisputeCount(org, this.locale);
                disputeMap.put(org, dc);
//                System.err.println("Fetched Dispute: "+locale+"/"+org+"="+dc.toString());
            }
            return dc;
        }
        
    }
    
    Map<CLDRLocale,CachedVettingData> cachedData = new HashMap<CLDRLocale,CachedVettingData>();
    private CachedVettingData getCachedLocaleData(CLDRLocale locale) {
        synchronized(this) {
            CachedVettingData vd = cachedData.get(locale);
            if(vd == null || !vd.isValid()) {
                if(vd!=null) // reduce noise first time thru
                      System.err.println(((vd==null)?"":"Re-")+"loading vet cache for " + locale);
                vd = new CachedVettingData(locale);
                cachedData.put(locale, vd);
            }
            return vd;
        }
    }
    public void deleteCachedLocaleData(CLDRLocale locale) {
        synchronized(this) {
            CachedVettingData vd = cachedData.get(locale);
            if(vd!=null && !vd.isValid()) {
            	cachedData.remove(vd);
            }
        }
    }
    
    public int getOrgDisputeCount(String org, CLDRLocale locale) {
        synchronized(conn) {
            return getCachedLocaleData(locale).getOrgDisputeCount(org);
        }
    }
    private int handleGetOrgDisputeCount(String org, CLDRLocale locale) {
        int count = 0;
        synchronized(conn) {
            try {
                orgDisputePathCount.setString(1,org);
                orgDisputePathCount.setString(2,locale.toString());
                
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

    boolean queryOrgDispute(String org, CLDRLocale locale, int base_xpath) {
        if(getOrgDisputeCount(org,locale)==0) return false; // quick exit: if no disputes.
        boolean result = false;
        synchronized(conn) {
            try {
                orgDisputeQuery.setString(1,org);
                orgDisputeQuery.setString(2,locale.toString());
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
        final String org = ctx.session.user.voterInfo().getOrganization().name();
        if(loc.equals("")) {
            synchronized(conn) {
                try {
                    orgDisputeLocs.setString(1,org);
                    
                    ResultSet rs = orgDisputeLocs.executeQuery();
                    if(rs.next()) {
                        ctx.println("<h4>Vetting Disputes for "+ctx.session.user.org+" ("+org+")</h4>");
                        
                        do {
                            loc = rs.getString(1);
                            int cnt = rs.getInt(2);
                            CLDRLocale cloc = CLDRLocale.getInstance(loc);
                            sm.printLocaleLink(ctx,cloc,cloc.getDisplayName(ctx.displayLocale));
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
        Map m = new  TreeMap ();
        Set<String> badLocales = new TreeSet<String>(); 
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
                rs = s.executeQuery("select "+CLDR_RESULT+".locale,"+CLDR_RESULT+".base_xpath from "+CLDR_RESULT+" where ("+CLDR_RESULT+".type="+RES_ERROR+")");
             } else {
                rs = s.executeQuery("select "+CLDR_RESULT+".locale,"+CLDR_RESULT+".base_xpath from "+CLDR_RESULT+" where ("+CLDR_RESULT+".type>"+RES_INSUFFICIENT+") AND ("+CLDR_RESULT+".type<="+RES_BAD_MAX+")");
            }
			while(rs.next()) {
				n++;
				String aLoc = rs.getString(1);
				int aXpath = rs.getInt(2);
				String path = sm.xpt.getById(aXpath);
				String theMenu = PathUtilities.xpathToMenu(path);
                
				if(theMenu==null) {
				    ctx.println("<div class='ferrbox'>Couldn't find menu for " + path + " ("+aLoc+":"+aXpath+")</div><br>");
				    theMenu="unknown";
				}
				Hashtable ht = (Hashtable)m.get(aLoc);
				if(ht==null) {
					locs++;
					ht = new Hashtable();
					m.put(aLoc,ht);
					badLocales.add(sm.getLocaleDisplayName(CLDRLocale.getInstance(aLoc)));
				}
				Set st = (Set)ht.get(theMenu);
				if(st==null) {
					st = new TreeSet();
					ht.put(theMenu,st);
				}
				st.add(path);
			}
			ctx.println("<hr>"+n+" disputed total in " + m.size() + " locales.<br>");			
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
        LocaleTree lm = sm.getLocaleTree();
        
        Iterator i;
        
//        i = lm.keySet().iterator();
        //i = m.keySet().iterator();
        i = badLocales.iterator();
		for(;i.hasNext();) {
            String locName = (String)i.next();
            String loc = sm.getLocaleCode(locName).toString();
            if(loc==null) loc = locName;
			Hashtable ht;
			ht = (Hashtable)m.get(loc);
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
            
            if(totalbad==0 && genCount==0) {
                if(sm.isUnofficial) {
                    ctx.println("<tr class='row"+(nn++ % 2)+"'>");
                    ctx.print("<th align='left'>"+totalbad+"</th>");
                    ctx.print("<th class='hang' align='left'>");
                    sm.printLocaleLink(subCtx,CLDRLocale.getInstance(loc),new ULocale(loc).getDisplayName().replaceAll("\\(",
                            "<br>(")); // subCtx = no 'do' portion, for now.
                    ctx.println("</th>");
                    ctx.println("</tr>");
                }
                continue;
            }
            
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
			sm.printLocaleLink(subCtx,CLDRLocale.getInstance(loc),new ULocale(loc).getDisplayName().replaceAll("\\(",
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
                        "_="+loc+"&amp;x="+theMenu+ 
                                    /* "&amp;only=disputed"+ */  // disputed only is broken.
                                "#"+DataSection.CHANGES_DISPUTED+"'>"+
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
            
            SurveyMain.UserLocaleStuff uf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
            CLDRFile cf = uf.cldrfile;
            if(cf == null) {
                throw new InternalError("CLDRFile is null!");
            }
            XMLSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC) + " @ " + ctx.getLocale() );
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
    
    Hashtable<CLDRLocale, Reference<DataTester>> hash = new Hashtable<CLDRLocale, Reference<DataTester>>();

    public boolean test(CLDRLocale locale, int xpath, int fxpath, String value) {
        return test(locale, sm.xpt.getById(xpath), sm.xpt.getById(fxpath), value);
    }
    
    public boolean test(CLDRLocale locale, String xpath, String fxpath, String value) {
        DataTester tester = get(locale);
        return tester.test(xpath, fxpath, value);
    }

    private DataTester get(CLDRLocale locale) {
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
            XMLSource dbSource = sm.makeDBSource(conn, null, locale);
//            CLDRDBSourceFactory.vettingMode(sm.vet);
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
        
        private DataTester(CLDRLocale locale) 
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
    
    /** Old version **/
    public void writeXpaths(PrintWriter out, String ourDate, Set<Integer> xpathSet) {
        out.println("<xpathTable host=\""+SurveyMain.localhost()+"\" date=\""+ourDate+"\"  count=\""+xpathSet.size()+"\" >");
        writeXpathFragment(out, xpathSet);
        out.println("</xpathTable>");
    }

    public void writeXpathFragment(PrintWriter out, Set<Integer> xpathSet) {
        for(int path : xpathSet) {
            out.println("<xpath id=\""+path+"\">"+xmlescape(sm.xpt.getById(path))+"</xpath>");
        }
    }

    public void writeXpaths(PrintWriter out, String ourDate, boolean xpathSet[]) {
        out.println("<xpathTable host=\""+SurveyMain.localhost()+"\" max=\""+xpathSet.length+"\" date=\""+ourDate+"\" >");
        writeXpathFragment(out, xpathSet);
        out.println("</xpathTable>");
    }

    public void writeXpathFragment(PrintWriter out, boolean xpathSet[]) {
        for(int path=0;path<xpathSet.length;path++) {
            if(xpathSet[path]) {
                out.println("<xpath id=\""+path+"\">"+xmlescape(sm.xpt.getById(path))+"</xpath>");
            }
        }
    }
    
    static int xpathMax=0;

    /**
     * Write a single vote file
     * @param conn2 DB connection to use
     * @param source source of current locale
     * @param file CLDRFile to read from
     * @param ourDate canonical date for generation
     * @throws SQLException 
     */
    public boolean[] writeVoteFile(PrintWriter out, Connection conn2, XMLSource source, CLDRFile file, String ourDate, boolean xpathSet[]) throws SQLException {
        boolean embeddedXpathTable = false;
        
        if(xpathSet == null) {
//            xpathSet = new TreeSet<Integer>();
            xpathSet = new boolean[xpathMax];
            embeddedXpathTable=true;
        }

        String locale = source.getLocaleID();
        XPathParts xpp = new XPathParts(null,null);
        boolean isResolved = source.isResolving();
        String oldVersion = SurveyMain.getOldVersion();
        String newVersion = SurveyMain.getNewVersion();
        out.println("<locale-votes host=\""+SurveyMain.localhost()+"\" date=\""+ourDate+"\" "+
                "oldVersion=\""+oldVersion+"\" currentVersion=\""+newVersion+"\" "+
                "resolved=\""+isResolved+"\" locale=\""+locale+"\">");

        PreparedStatement resultsByBase = conn2.prepareStatement(
                   "select cldr_output.output_xpath,cldr_output.output_full_xpath," +
                       "cldr_output.status,cldr_output.data_xpath from " +
                       "cldr_output" +
                       " where cldr_output.locale=? and cldr_output.base_xpath=?");
        PreparedStatement votesByValue = conn2.prepareStatement("select submitter from cldr_vet where locale=? and vote_xpath=?");
        /*
         *                                                         "(locale VARCHAR(20) NOT NULL, " +
                                                                "base_xpath INT NOT NULL," +
                                                                "output_xpath INT NOT NULL," +
                                                                "output_full_xpath INT NOT NULL," +
                                                                "data_xpath INT NOT NULL, " +
                                                                "status SMALLINT)");

         */
        resultsByBase.setString(1, locale);
        votesByValue.setString(1, locale);
                
        Statement basesByLocale = conn2.createStatement();
//        ResultSet base_result = basesByLocale.executeQuery("select distinct cldr_vet.base_xpath from cldr_vet where cldr_vet.locale='"+locale+"'");
        ResultSet base_result = basesByLocale.executeQuery("select base_xpath from cldr_result where locale='"+locale+"'");

        
        int n=0;
        
        while(base_result.next()) {
            n++;
            int baseXpath = base_result.getInt(1);
            boolean hadRow = false;
            resultsByBase.setInt(2, baseXpath);
            ResultSet results = resultsByBase.executeQuery();
            boolean sawBasePath = false;
            while(results.next()) {
                int outPath = results.getInt(1);
                int outFull = results.getInt(2);
                if(outFull>=xpathSet.length || baseXpath>=xpathSet.length) {
                    if(xpathMax==0) {
                        xpathMax=sm.xpt.count()+XPathTable.CHUNKSIZE;
                    }
                    int max = java.lang.Math.max(outFull, baseXpath);
                    xpathMax = java.lang.Math.max(max+XPathTable.CHUNKSIZE, xpathMax);
                    boolean newXpathSet[] = new boolean[xpathMax];
                    System.arraycopy(xpathSet, 0, newXpathSet, 0, xpathSet.length);
                    xpathSet=newXpathSet;
                    System.err.println("VV:XPT:Expand to "+xpathMax);
                }
                if(!hadRow) {
                    hadRow=true;
                    out.println("\t<row baseXpath=\""+baseXpath+"\">"); // result=\""+resultXpath+"\">");
                    //xpathSet.add(baseXpath);
                    xpathSet[baseXpath]=true;
                    //out.println("\t\t<xpath type=\"base\" id=\""+baseXpath+"\">"+xmlescape(sm.xpt.getById(baseXpath))+"</xpath>");
                }
                String outFullString = sm.xpt.getById(outFull);
//                int outStatus = results.getInt(3);
//                Status status = Status.find(outStatus);
                int outDataPath = results.getInt(4);
                boolean isBase = (outPath==baseXpath) ;
                
                if (isBase) { sawBasePath = true; }
//                boolean hadVotes = false;
                xpp.clear();
                xpp.initialize(outFullString);
                String lelement = xpp.getElement(-1);
                String iDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
                if(iDraft == null || iDraft.length()==0) {
                    iDraft = "approved";
                }
                //xpathSet.add(outFull);
                xpathSet[outFull]=true;
//                xpathSet.add(outDataPath);
                out.println("\t\t<item xpath=\""+outFull+"\" type=\""+
                        (isBase?"optimal":"proposal")+"\" id=\""+outDataPath+"\" status=\""+iDraft+"\""+">");
                if(outDataPath==baseXpath) {
                    String oxpath = source.getFullPathAtDPath(sm.xpt.getById(outDataPath)); // ,false
                    String eDraft = null;
                    if(oxpath == null) {
                        eDraft = "unknown";
                    } else {
                        xpp.clear();
                        xpp.initialize(oxpath);
                        String lelement2 = xpp.getElement(-1);
                        /* all of these are always at the end */
    //                    String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
                        eDraft = xpp.findAttributeValue(lelement2,LDMLConstants.DRAFT);
                        if(eDraft == null) {
                            eDraft = "confirmed";
                        }
                    }
                    out.println("\t\t\t<old status=\""+eDraft+"\"/>"); // blue star
                }
//                out.print("<xpath >"+xmlescape(outFullString)+"</xpath>");

                votesByValue.setInt(2, outDataPath);

                ResultSet votes = votesByValue.executeQuery();
                while(votes.next()) {
                    int submitter = votes.getInt(1);
                    out.println("\t\t\t<vote user=\""+submitter+"\"/>");
                }
                
                out.println("\t\t</item>");
            }
            if(hadRow) {
                out.println("\t</row>");
            }
        }
        if(embeddedXpathTable) {
            out.println(" <xpathTable max=\""+xpathSet.length+"\">");
            writeXpathFragment(out,xpathSet);
            out.println(" </xpathTable>");
        }
        out.println("</locale-votes>");
        return xpathSet;
    }

    private String xmlescape(String str) {
        if(str.indexOf('&')>=0) {
            return str.replaceAll("&", "\\&amp;");
        } else {
            return str;
        }
    }

}
