//
//  Vetting.java
//  fivejay
//
//  Created by Steven R. Loomis on 04/04/2006.
//  Copyright 2006 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import com.ibm.icu.util.ULocale;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LDMLUtilities;

import com.ibm.icu.dev.test.util.ElapsedTimer;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * This class does the calculations for which item wins a vetting,
 * and also manages some notifications,
 * and also records which votes are cast
 */
public class Vetting {
    private static java.util.logging.Logger logger;
    
    public static final String CLDR_RESULT = "cldr_result"; /** constant for table access **/
    public static final String CLDR_VET = "cldr_vet"; /** constant for table access **/
    public static final String CLDR_INTGROUP = "cldr_intgroup"; /** constant for table access **/
    public static final String CLDR_STATUS = "cldr_status"; /** constant for table access **/
    
    
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
    public static final int RES_DISPUTED        = 4;      /** 4:D disputed item **/

    public static final int RES_BAD_MAX = RES_DISPUTED;  /** Data is OK (has a valid xpath in final data) if (type > RES_BAD_MAX) **/
    public static final int RES_BAD_MASK  = RES_NO_VOTES|RES_INSUFFICIENT|RES_DISPUTED;  /** bitmask for testing 'BAD' (invalid) **/
    
    
    public static final int RES_ADMIN           = 8;   /** 8:A Admin override **/
    public static final int RES_TC              = 16;  /** 16:T  TC override (or, TC voted at least..) **/
    public static final int RES_GOOD            = 32;  /** 32:G good - normal **/
    public static final int RES_UNANIMOUS       = 64;  /** 64:U  unamimous **/
    public static final int RES_NO_CHANGE       = 128; /** 128:N no new data **/
    public static final int RES_REMOVAL         = 256; /** 256: result: removal **/

    public static final String RES_LIST = "0IDATGUNR"; /** list of strings for type 2**n.  @see typeToStr **/
	public static final String TWID_VET_VERBOSE = "Vetting_Verbose"; /** option string for toggling verbosity in vetting **/
	boolean VET_VERBOSE=false; /** option boolean for verbose vetting, defaults to false **/
    
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
        synchronized(conn) {
            String sql = null;
//            logger.info("Vetting DB: initializing...");
            if(!sm.hasTable(conn, CLDR_RESULT)) {
                logger.info("Vetting DB: setting up " + CLDR_RESULT);
                Statement s = conn.createStatement();
                sql = "create table " + CLDR_RESULT + " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                        "locale VARCHAR(20) NOT NULL, " +
                                                        "base_xpath INT NOT NULL, " +
                                                        "result_xpath INT, " +
                                                        "modtime TIMESTAMP, " +
                                                        "type SMALLINT NOT NULL, "+
                                                        "old_result_xpath INT, "+
                                                        "old_modtime INT, "+
                                                        "old_type SMALLINT, "+
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
                s.execute("create table " + CLDR_VET + "(id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
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
    PreparedStatement rmVote = null;
    PreparedStatement dataByBase = null;
    PreparedStatement insertResult = null;
    PreparedStatement queryResult = null;
    PreparedStatement countResultByType = null;
    PreparedStatement queryVoteId = null;
    PreparedStatement updateVote = null;
    PreparedStatement rmResult = null;
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
                "select xpath,origxpath,alt_type FROM CLDR_DATA WHERE " + 
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
            insertResult = prepareStatement("insertResult", 
                "insert into CLDR_RESULT (locale,base_xpath,result_xpath,type,modtime) values (?,?,?,?,CURRENT_TIMESTAMP)");
            rmResult = prepareStatement("rmResult", 
                "delete from CLDR_RESULT where (locale=?)AND(base_xpath=?)");
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
                "update CLDR_RESULT set result_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where id=?");
            intQuery = prepareStatement("intQuery",
                "select last_sent_nag,last_sent_update from CLDR_INTGROUP where intgroup=?");
            intAdd = prepareStatement("intAdd",
                "insert into CLDR_INTGROUP (intgroup) values (?)");
            intUpdateNag = prepareStatement("intUpdateNag",
                "update CLDR_INTGROUP  set last_sent_nag=CURRENT_TIMESTAMP where intgroup=?");
            intUpdateUpdate = prepareStatement("intUpdateUpdate",
                "update CLDR_INTGROUP  set last_sent_update=CURRENT_TIMESTAMP where intgroup=?");
            queryValue = prepareStatement("queryValue",
                "SELECT value FROM " + CLDRDBSource.CLDR_DATA +
                        " WHERE locale=? AND xpath=?");
			highestVetter = prepareStatement("highestVetter",
				"select CLDR_USERS.userlevel from CLDR_VET,CLDR_USERS where CLDR_USERS.id=CLDR_VET.submitter AND CLDR_VET.locale=? AND CLDR_VET.vote_xpath=? ORDER BY CLDR_USERS.userlevel");
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
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
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
    public Set gatherVotes(String locale, String base_xpath) {
        int base_xpath_id = sm.xpt.getByXpath(base_xpath);
        synchronized(conn) {
            try {
                queryVoteForXpath.setString(1, locale);
                queryVoteForXpath.setInt(2, base_xpath_id);
                ResultSet rs = queryVoteForXpath.executeQuery();
               // System.err.println("Vf: " + base_xpath_id + " / " + base_xpath);
                Set result = null;
                while(rs.next()) {
                    if(result == null) {
                        result = new HashSet();
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

    /**
     * update all vetting results
     * @return the number of results changed
     */
    public int updateResults() {
        System.err.println("******************** NOTE: updateResults() doesn't send notifications yet.");
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
            int count = updateResults(localeName,types);
            tcount += count;
            if(count>0) {
                lcount++;
                System.err.println("updateResults("+localeName+ " ("+count+" updated, "+typeToStr(types[0])+") - "+i+"/"+nrInFiles+") took " + et2.toString());
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
        System.err.println("Done updating "+tcount+" results votes ("+lcount + " locales). Elapsed:" + et.toString());
        System.err.println("******************** NOTE: updateResults() doesn't send notifications yet.");
        return tcount;
    }
    
    /**
     * update the results of a specific locale, without caring what kind of results were had.  This is a convenience
     * function so you don't have to new up an array.
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
    
    /**
     * update the results of a specific locale, and return the bitwise OR of all types of results had
     * @param locale which locale
     * @param type an OUT parameter, must be a 1-element ( new int[1] ) array. input value doesn't matter. on output, 
     * contains the bitwise OR of all types of vetting results which were found.
     * @return number of results changed
     **/
    public int updateResults(String locale, int type[]) {
		VET_VERBOSE=sm.twidBool(TWID_VET_VERBOSE); // load user prefs: should we do verbose vetting?
        int ncount = 0; // new count
        int ucount = 0; // update count
        int updates = 0;
        int base_xpath=-1;
        // two lists here.
        //  #1  results that are missing (unique CLDR_DATA.base_xpath but no CLDR_RESULT).  Use 'insert' instead of 'update', no 'old' data.
        //  #2  results that are out of date (CLDR_VET with same base_xpath but later modtime.).  Use 'update' instead of 'insert'. (better yet, use an updatable resultset).
        synchronized(conn) {
            try {
                //dataByUserAndBase.setString(3, locale);
                missingResults.setString(1,locale);
                //insertVote.setString(1,locale);
                //insertVote.setInt(5,VET_IMPLIED);
                
                // for updateResults(id, ...)
                dataByBase.setString(1,locale);
                queryVoteForBaseXpath.setString(1,locale);
                insertResult.setString(1,locale);
                
                
                ResultSet rs = missingResults.executeQuery();
                while(rs.next()) {
                    ncount++;
                    base_xpath = rs.getInt(1);
                    
                    int rc = updateResults(-1, locale, base_xpath);

                    updates |= rc;
                }
                if(ncount > 0) {
                    int uscnt = updateStatus(locale, true);// update results
                    if(uscnt>0) {
                        System.err.println("updated " + uscnt + " statuses, due to implied vote\n");
                    } else {
                        System.err.println("No statuses updated due to impliedvote.\n");
                    }
                    conn.commit();
                }
                
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
                        System.err.println("updated " + uscnt + " statuses, due to vote change\n");
                    } else {
                        System.err.println("updated " + uscnt + " statuses, due to vote change??\n");
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
    
    /**
     * This class represents a particular item that can be voted for,
     * a single "contest" if you will.
     * TODO: implement the Chadless version of the survey tool.
     */
    class Chad {
        public int xpath; /** xpath of this item **/
        public Set voters = new HashSet(); /** Set of users which voted for this. **/
        
        // calculated:
        public boolean admin_voted_for = false; /** did an admin vote for it ? **/
        public boolean quorum = false; /** do we have a quorum? **/
        public boolean someone_voted_for = false; /** did anyone at all vote for it? **/
        public boolean removal = false; /** is this a vote for Removal? **/
        
        /**
         * construct a Chad over a certain xpath id
         * @param x the id
         */
        Chad(int x) {
            xpath = x;
        }
        
        /**
         * Register a vote for a particular user
         * @param u the user
         */
        void vote(UserRegistry.User u) {
            voters.add(u);
            
            if(UserRegistry.userIsVetter(u)) {
                someone_voted_for=true;
            }
            
            if(UserRegistry.userIsAdmin(u)) {
                admin_voted_for = true;
                quorum = true;
			    if(VET_VERBOSE) System.err.println(" +Quorum: admin  " + u + " --> "+toString());
            } else if(UserRegistry.userIsExpert(u)) {
                quorum = true;
			    if(VET_VERBOSE) System.err.println(" +Quorum: expert " + u + " --> "+toString());
            } else if(UserRegistry.userIsVetter(u)) {
                /* did at least one vetter from another user vote for it? */
                if(!quorum) {
                    for(Iterator i = voters.iterator();!quorum&&i.hasNext();) {
                        UserRegistry.User them = (UserRegistry.User)i.next();
                        if(!them.org.equals(u.org) &&
                            UserRegistry.userIsVetter(them)) {
                            quorum=true;
							if(VET_VERBOSE) System.err.println(" +Quorum: got " + u + " and " + them + " --> "+toString());
							return;
                        }
                    }
                }
            }
        }
        
        /**
         * return a Chad in string format
         * for debugging
         */
        public String toString() {
            String rs ="";
            if(quorum) {
                rs = "QUORUM: ";
            } else {
                rs = "        ";
            }
            if(admin_voted_for) {
                rs = rs + " [ADMIN] ";
            }
            if(someone_voted_for) {
                rs = rs + " [V] ";
            }
            for(Iterator i = voters.iterator();i.hasNext();) {
                UserRegistry.User them = (UserRegistry.User)i.next();
                rs = rs + "{"+them.toString()+"}";
                if(i.hasNext()) {
                    rs = rs + ", ";
                }
            }
            rs = rs + " - " + voters.size()+"voters";
            return rs + " #"+xpath;
        }
    };
    
     
    Map chads = new HashMap();

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
    private int updateResults(int id, String locale, int base_xpath) throws SQLException {
        chads.clear();

        int resultXpath = -1;
        int type = -1;
        int fallbackXpath = -1;

        boolean disputed = false; 
        queryValue.setString(1,locale);
        
        // Step 0: gather all votes
        {
            queryVoteForBaseXpath.setInt(2,base_xpath);
            ResultSet rs = queryVoteForBaseXpath.executeQuery();
            int count =0;
            while(rs.next()) {
                count++;
                int submitter = rs.getInt(1);
                int vote_xpath = rs.getInt(2);
                if(vote_xpath==-1) {
                    continue; // abstention
                } 
                Integer vote_int = new Integer(vote_xpath);
                Chad c = (Chad)chads.get(new Integer(vote_xpath));
                if(c==null) {
                    c = new Chad(vote_xpath);
                    
                    // Is it removal?   either: #1 base_xpath=vote_xpath but no data (i.e. 'inherited'), or #2 non-base xpath, but value="". '(empty)'
                    queryValue.setInt(2,vote_xpath);
                    ResultSet crs= queryValue.executeQuery();
                    if(!crs.next()) {
//                        if(vote_xpath==base_xpath){
///*srl, et al*/              System.err.println(locale+":"+vote_xpath + " = remmoval: MISSING value in base_xpath=vote_xpath");
                        c.removal=true; // always a removal.
                    } else {
                        String v = crs.getString(1);
                        if(v.length()==0) {
                            if(vote_xpath!=base_xpath){
//                                System.err.println(locale+":"+vote_xpath + " = remmoval: 0-length value in vote_xpath");
                                c.removal=true;
                            } else {
//                                System.err.println(locale+":"+vote_xpath + " = no err - 0 length value.");
                            }
                        }
                    }
                    crs.close();
                }
                UserRegistry.User u = sm.reg.getInfo(submitter);
                c.vote(u);
                chads.put(vote_int, c); // TODO: should be able to do this only when it wasnt present
            }
            if(count>0) {
                //System.err.println(locale+":"+base_xpath+" - Collected "+count+" votes in " + chads.size() + " chads");                
            }
        }
        
        if((type==-1) && !chads.isEmpty()) { // no resolution AND someone did vote.
            int number_voted_for=0;
            boolean sawQuorum = false;
            boolean tcOverride = false;
            Chad quorumChad = null;
			Chad adminChad = null;
            for(Iterator i=chads.values().iterator();i.hasNext();) { // see if any chads win.  Collect admin chad.  This could set DISPUTED. 
                Chad c = (Chad)i.next();
				if((adminChad == null) && c.admin_voted_for) {
					adminChad = c;
				}
                if(c.someone_voted_for) {
                    number_voted_for++;
                    if(number_voted_for>1) {
                        type = RES_DISPUTED;  // if more than one chad has a legit vote - disputed.
                    }
                }
                if(VET_VERBOSE) System.err.println("  "+c);
                if(c.quorum) {
                    if(sawQuorum) {
                        type = RES_DISPUTED; // Note: don't handle TC or admin ability to override, yet. Probably handle with an admin supervote.
                    } else {
                        sawQuorum = true;
                        quorumChad = c;
                    }
                }
            }
			if((type==RES_DISPUTED)&&(adminChad != null)) { // If it was already unanimous - no need to consider it an override. Keep the noise down.
				type = RES_ADMIN;
                resultXpath = adminChad.xpath;
                fallbackXpath = resultXpath;
				if(adminChad.removal) {
					type = RES_REMOVAL; // admin override removal : no special type for this.
				}
			}
            if(sawQuorum && (type==-1)) {
                // We have a winner.
                if(quorumChad.removal) {
///*srl*/                    System.err.println("RESULT: "+locale+":"+base_xpath+" = RES_REMOVAL");
                    type = RES_REMOVAL; // quorum is to remove an item. Needed to suppress base_xpath from iterator()
                } else if(chads.size()==1) {
                    type = RES_UNANIMOUS; // not shown if TC also voted.
                } else if(quorumChad.admin_voted_for) {
                    type = RES_ADMIN;
                } else {
                    type = RES_GOOD; // we have a winner, anyways.
                }
                resultXpath = quorumChad.xpath;
                fallbackXpath = resultXpath;
            }
            
            if((type==-1)&&(sawQuorum == false) && !chads.isEmpty()) {
                type = RES_INSUFFICIENT; // Some people voted, but no quorum.
            }
        } 
//System.err.println(base_xpath+": resu="+resultXpath);
        if(resultXpath == -1) { // No type set yet and no votes
            // Step 1: gather the winner (if any)
              //              "select xpath,origxpath,value,type,alt_type,submitter,modtime FROM CLDR_DATA WHERE " + 
              //          "(locale=?) AND (base_xpath=?)");
              // NB this is too heavyweight for this common case!
            dataByBase.setInt(2,base_xpath);
            boolean sawProposed = false; // any 'proposed' items at all?
            boolean sawExisting = false; // any existing items at all?
            
            ResultSet rs = dataByBase.executeQuery();
            int updated=0;
            int count = 0;
            int existingXpath = -1;
            
            while(rs.next()) {
                count++;
                int xpath       = rs.getInt(1);
                int orig_xpath  = rs.getInt(2);
                String alt_type = rs.getString(3);
                boolean altNull = rs.wasNull();
                boolean isDraft = false;
//    System.err.println(xpath+":"+orig_xpath+" - "+alt_type);
                if(orig_xpath!=xpath) {
                    if(sm.xpt.getById(orig_xpath).indexOf("[@draft=\"true\"]")!=-1) {
                        isDraft=true; /// altproposed doesn't mark drafts
                    }
                }

                if((orig_xpath==base_xpath)&&(fallbackXpath == -1)) {
                    fallbackXpath = base_xpath; // fall back to base
                }
                
                if(!altNull) {
                    String typeAndProposed[] = LDMLUtilities.parseAlt(alt_type);
                    String altProposed = typeAndProposed[1];
                    String altType = typeAndProposed[0];
                
                    if(((altProposed==null) ||
                        altProposed.length()==0)&&
                            !isDraft) {
                        sawExisting = true; // non-proposed alternate of some sort.
                        existingXpath = xpath;
                    } else {
                        sawProposed = true;
                    }
                } else { // alt was null.
                    if(!isDraft) {
                        sawExisting = true;
                        existingXpath = xpath;
                    } else {
                        sawProposed = true;
                    }
                }
            }
            
//System.err.println(base_xpath+": fb="+fallbackXpath+", x="+existingXpath);
            if(fallbackXpath == -1) {
                fallbackXpath = existingXpath;
            }
                        
            if(type==-1) {
                // no other resolution - was there an existing item?
                if((count==1) && sawExisting && !sawProposed) {
                    resultXpath = existingXpath;
                    //System.err.println(base_xpath + " - no new data (existing " + resultXpath+")");
                    type = RES_NO_CHANGE; // Existing data.. nobody voted.. we'll take it.
                } else {
                    //System.err.println("V: "+locale+":"+base_xpath+"=" + "? - count="+count+", sawX="+new Boolean(sawExisting)+", sawP="+new Boolean(sawProposed)+".");
                    type = RES_NO_VOTES; // Nobody voted.. something was proposed, though.
                }
            }
        } // end no-votes
        
        // resulted in -1 and was bad - sanity check. Should have fallen back to something. 
        if((resultXpath==-1)&&
           (type>RES_BAD_MAX)) {
            throw new RuntimeException("Internal Error: Can't update "+locale+":"+base_xpath+
                    " - no resultXpath and type is " + typeToStr(type));
        }
        
        if(type==-1) {
            // another sanity check. Should have had a resolution here.
            throw new RuntimeException("Internal Error: Can't update "+locale+":"+base_xpath+" - no type");
        }
        
        // --------
        // Now, update the vote results
        int setXpath = resultXpath;
        
        if(setXpath==-1) {
            // fallback situation
            setXpath = fallbackXpath;
        }
        
        if(id == -1) { 
            // Not an existing vote:  insert a new oen
            // insert 
        // insertResult: "insert into CLDR_RESULT (locale,base_xpath,result_xpath,type,modtime) values (?,?,?,?,CURRENT_TIMESTAMP)");
            insertResult.setInt(2,base_xpath);
//System.err.println(base_xpath+"::fb="+fallbackXpath+", rx="+resultXpath);
            if(setXpath != -1) {
                insertResult.setInt(3,setXpath);
            } else {
                insertResult.setNull(3,java.sql.Types.SMALLINT); // no fallback.
            }
            insertResult.setInt(4,type);
            /*
            if((type != RES_NO_CHANGE) && // boring ones..
                (type != RES_NO_VOTES)) {
                System.err.println("V: "+locale+":"+base_xpath+"=" + resultXpath+ " ("+typeToStr(type)+")");
            }
            */
            int res = insertResult.executeUpdate();
            //conn.commit(); // should this be not done, for perf?
            if(res != 1) {
                throw new RuntimeException(locale+":"+base_xpath+"=" + resultXpath+ " ("+typeToStr(type)+") - insert failed.");
            }
            return type;
        } else {
            // existing vote: get the old one
            // ... for now, just do an update
            // update CLDR_RESULT set vote_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where id=?
            updateResult.setInt(1, setXpath);
            updateResult.setInt(2, type);
            updateResult.setInt(3, id);
            int res = updateResult.executeUpdate();
            if(res != 1) {
                throw new RuntimeException(locale+":"+base_xpath+"@"+id+"="+resultXpath+ " ("+typeToStr(type)+") - update failed.");
            }
            return type;
        }
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
                    if(type[0]>RES_BAD_MAX) {
                        rv = rs.getInt(1);
                    } else {
                        rv = -1; // F
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
    int updateStatus() { // updates MISSING status
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
                MailSender.sendMail(smtp,from,u.email, title, s);
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
    void doNag() {
        Map mailBucket = new HashMap(); // mail bucket: 
    
        Map intGroups = sm.getIntGroups();
        Map intUsers = sm.getIntUsers(intGroups);
        
        System.err.println("--- nag ---");
        
        for(Iterator li = intGroups.keySet().iterator();li.hasNext();) {
            String group = (String)li.next();
            Set s = (Set)intGroups.get(group);            
            doNag(mailBucket, (Set)intUsers.get(group), group, s);
        }
        
        if(mailBucket.isEmpty()) {
            System.err.println("--- nag: nothing to send.");
        } else {
            int n= sendBucket(mailBucket, "CLDR Unresolved Issues Report");
            System.err.println("--- nag: " + n + " emails sent.");
        }
    }
    
    /** 
     * compose nags for one group
     * @param mailBucket IN/OUT of mail waiting to go out
     * @param intUsers interested users in this group
     * @param group the interest group being processed
     * @param s the list of locales contained in interest group 'group'
     */
    void doNag(Map mailBucket, Set intUsers, String group, Set s) {
        // First, are there any problems here?
        String complain = null;
        
        if((intUsers==null) || intUsers.isEmpty()) {
            // if noone cares ...
            return;
        }
        
        boolean didPrint =false;
        for(Iterator li=s.iterator();li.hasNext();) {
            String loc = (String)li.next();
            
            int locStatus = status(loc);
            if((locStatus&RES_BAD_MASK)>0) {
                int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
                int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
                int numDisputed = countResultsByType(loc,RES_DISPUTED);
                
                if(complain == null) {
//                    System.err.println(" -nag: " + group);
                    complain = "\n\n* Group '" + group + "' ("+new ULocale(group).getDisplayName()+")  needs attention:  ";
                }
//                System.err.println("  -nag: " + loc + " - " + typeToStr(locStatus));
                String problem = "";
                if((numNoVotes+numInsufficient)>0) {
                    problem = problem + " INSUFFICIENT VOTES: "+(numNoVotes+numInsufficient)+" ";
                }
                if(numDisputed>0) {
                    problem = problem + " DISPUTED VOTES: "+numDisputed+"";
                }

                complain = complain + "\n "+ new ULocale(loc).getDisplayName() + " - " + problem + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc;
            }
        }
        if(complain != null) {
            for(Iterator li = intUsers.iterator();li.hasNext();) {
                UserRegistry.User u = (UserRegistry.User)li.next();
                Integer intid = new Integer(u.id);
                String body = (String)mailBucket.get(intid);
                if(body == null) {
                    body = "The following is an automatic message, periodically generated to update vetters on the progress on their locales. We are working on a short time schedule, so we'd appreciate your looking at the cases below.\r\n"+
                    "\r\nFor more information, see http://unicode.org/cldr/wiki?VettingProcess\nYou will need to be logged-in before making changes.\r\n\r\n";
                }
                body = body + complain + "\n";
                mailBucket.put(intid,body);
            }
        }
    }


    /**
     * Send out dispute nags for one organization.
     * @param  message your special message
     * @param org the organization
     * @return number of mails sent.
     */
    int doDisputeNag(String message, String org) {
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
            if((locStatus&RES_DISPUTED)>0) {  // RES_BAD_MASK
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
                    body = message + "\n\nYou will need to be logged-in before making changes at these URLs.\r\n\r\n";
                }
                body = body + complain + "\n";
                mailBucket.put(intid,body);
            }
        }
    }

    /**
     * empty function
     * TODO: why is this function empty?
     */
    void doUpdate() {
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
	
    /**
     * Show the 'disputed' page.
     * @param ctx webcontext for IN/OUT stuff
     */
    void doDisputePage(WebContext ctx) {
        Map m = new TreeMap();
        int n = 0;
        int locs=0;
        synchronized(conn) {
         try {
                // select CLDR_RESULT.locale,CLDR_XPATHS.xpath from CLDR_RESULT,CLDR_XPATHS where CLDR_RESULT.type=4 AND CLDR_RESULT.base_xpath=CLDR_XPATHS.id order by CLDR_RESULT.locale
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("select CLDR_RESULT.locale,CLDR_RESULT.base_xpath from CLDR_RESULT where CLDR_RESULT.type=4");
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
			ctx.println("<hr>"+n+" disputed items total in " + m.size() + " locales.<br>");			
         } catch ( SQLException se ) {
            String complaint = "Vetter:  couldn't do DisputePage - " + SurveyMain.unchainSqlException(se);
            logger.severe(complaint);
            se.printStackTrace();
            throw new RuntimeException(complaint);
         }
        }

		for(Iterator i = m.keySet().iterator();i.hasNext();) {
			String loc = (String)i.next();
			ctx.println("<h4>");
			sm.printLocaleLink(ctx,loc,new ULocale(loc).getDisplayName());
			ctx.println("</h4>");
			ctx.println("<ul>");
			Hashtable ht = (Hashtable)m.get(loc);
			for(Iterator ii = ht.keySet().iterator();ii.hasNext();) {
				String theMenu = (String)ii.next();
				ctx.print("<li><a style='text-decoration: none;' href='"+ctx.base()+"?"+
					"_="+loc+"&amp;x="+theMenu+"&amp;only=disputed#"+DataPod.CHANGES_DISPUTED+"'>"+theMenu+"</a><pre>");
				for(Iterator iii = ((Set)ht.get(theMenu)).iterator();iii.hasNext();) {
					String xp = (String)iii.next();
					ctx.println(xp);
				}
				ctx.print("</pre></li>");
			}
			ctx.println("</ul>");
			/*
				if(theMenu != null) {
				}
				ctx.print(path);
				*/
		}
	}
}
