//
//  UserRegistry.java
//  fourjay
//
//  Created by Steven R. Loomis on 14/10/2005.
//  Copyright 2005-2008 IBM. All rights reserved.
//

package org.unicode.cldr.web;


import java.io.*;
import java.util.*;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * This class represents the list of all registered users.  It contains an inner class, UserRegistry.User, 
 * which represents an individual user.
 * @see UserRegistry.User
 * @see OldUserRegistry
 **/
public class UserRegistry {
    private static java.util.logging.Logger logger;
    // user levels
    public static final int ADMIN   = 0;  /** Administrator **/
    public static final int TC      = 1;  /** Technical Committee **/
    public static final int EXPERT  = 3;  /** Expert Vetter **/
    public static final int VETTER  = 5;  /** regular Vetter **/
    public static final int STREET  = 10; /** Guest Vetter **/
    public static final int LOCKED  = 999;/** Locked user - can't login **/
	
	public static final int LIMIT_LEVEL = 10000;  /** max level **/
	public static final int NO_LEVEL  = -1;  /** min level **/
    
    public static final String FOR_ADDING= "(for adding)"; /** special "IP" value referring to a user being added **/ 
    

    /**
     * List of all user levels - for UI presentation
     **/
    public static final int ALL_LEVELS[] = {
        ADMIN, TC, EXPERT, VETTER, STREET, LOCKED };
    
    /**
     * get a level as a string - presentation form
     **/
    public static String levelToStr(WebContext ctx, int level) {
        return level + ": (" + levelAsStr(level) + ")";
    }
    /**
     * get just the raw level as a string
     */
    public static String levelAsStr(int level) {
        String thestr = null;
        if(level <= ADMIN) { 
            thestr = "ADMIN";
        } else if(level <= TC) {
            thestr = "TC";
        } else if (level <= EXPERT) {
            thestr = "EXPERT";
        } else if (level <= VETTER) {
            thestr = "VETTER";
        } else if (level <= STREET) {
            thestr = "STREET";
        } else if (level == LOCKED) {
            thestr = "LOCKED";
        } else {
            thestr = "??";
        }
        return thestr;
    }
    
    
    PreparedStatement insertStmt = null;
    PreparedStatement importStmt = null;
    PreparedStatement queryStmt = null;
    PreparedStatement queryIdStmt = null;
    PreparedStatement queryEmailStmt = null;
    PreparedStatement updateInfoEmailStmt = null;
    PreparedStatement updateInfoNameStmt = null;
    PreparedStatement updateInfoPasswordStmt = null;
    PreparedStatement touchStmt = null;


    PreparedStatement removeIntLoc = null;
    PreparedStatement updateIntLoc = null;
    
    /**
     * This nested class is the representation of an individual user. 
     * It may not have all fields filled out, if it is simply from the cache.
     */
    public class User {
        public int    id;  // id number
        public int    userlevel=LOCKED;    // user level
        public String password;       // password
        public String email;    // 
        public String org;  // organization
        public String name;     // full name
        public java.sql.Timestamp last_connect;
        public String locales;
        public String intlocs = null;
        public String ip;
        public boolean equals(Object other ) {
            if(!(other instanceof User)) {
                return false;
            }
            User u = (User)other;
            return(u.id == id);
        }
        public void printPasswordLink(WebContext ctx) {
            UserRegistry.printPasswordLink(ctx, email, password);
        }
        public String toString() {
            return email + "("+org+")-" + levelAsStr(userlevel)+"#"+userlevel;
        }
        public String toHtml(User forUser) {
            if(forUser==null||!userIsTC(forUser)) {
                return "("+org+"#"+id+")";
            } else {
                return "<a href='mailto:"+email+"'>"+name+"</a>-"+levelAsStr(userlevel).toLowerCase();
            }
        }
        public int hashCode() { 
            return id;
        }
        /**
         * is the user interested in this locale?
         */
        public boolean interestedIn(CLDRLocale locale) {
            return UserRegistry.localeMatchesLocaleList(intlocs, locale);
        }
        
        /** 
         * List of interest groups the user is interested in.
         * @return list of locales, or null for ALL locales, or a 0-length list for NO locales.
         */
        public String[] getInterestList() {
            if(userIsExpert(this)) {
                if(intlocs == null || intlocs.length()==0) {
                    return null;
                } else {
                    if(intlocs.equals("none")) {
                        return new String[0];
                    }
                    return tokenizeLocale(intlocs);
                }
            } else if(userIsStreet(this)) {
                return tokenizeLocale(locales);
            } else {
                return new String[0];
            }
        }

        public final boolean userIsSpecialForCLDR15(CLDRLocale locale) {
            return false;
        }
//            if(locale.equals("be")||locale.startsWith("be_")) {
//                if( ( id == 315 /* V. P. */ ) || (id == 8 /* S. M. */ ) ) {
//                    return true;
//                } else {
//                    return false;
//                }
//            } else if ( id == 7 ) {  // Erkki
//                return true;
//            } else {
//                return false;
//            }
        
        /**
         * Convert this User to a VoteREsolver.VoterInfo. Not cached.
         */
        private VoterInfo createVoterInfo() {
            //VoterInfo(Organization.google, Level.vetter, &quot;J. Smith&quot;) },
            VoteResolver.Organization o = this.computeVROrganization();
            VoteResolver.Level l = this.computeVRLevel();
            Set<String> localesSet = new HashSet<String>();
            for(String s: tokenizeLocale(locales)) {
                localesSet.add(s);
            }
            VoterInfo v = new VoterInfo(o, l, this.name, localesSet);
            return v;
        }
        
        /**
         * Return the value of this voter info, out of the cache
         * @return
         */
        public VoterInfo voterInfo() {
            return getVoterToInfo(id);
        }
        
        /**
         * Convert the level to a VoteResolver.Level
         * @return VoteResolver.Level format
         */
        private VoteResolver.Level computeVRLevel() {
            switch (this.userlevel) {
            case ADMIN: return VoteResolver.Level.admin;
            case TC: return VoteResolver.Level.tc;
            case EXPERT: return VoteResolver.Level.expert;
            case VETTER: return VoteResolver.Level.vetter;
            case STREET: return VoteResolver.Level.street;
            case LOCKED: return VoteResolver.Level.locked;
            default:
                throw new IllegalArgumentException("Illegal VoteLevel on " + this.toString() +" - "+this.userlevel);
            }
       }
        
        /**
         * Convert the Organization into a VoteResolver.Organization
         * @return VoteResolver.Organization format
         */
        private VoteResolver.Organization computeVROrganization() {
            VoteResolver.Organization o = null;
            try {
                String arg = this.org
                                .replaceAll("Utilika Foundation", "utilika")
                                .replaceAll("Government of Pakistan - National Language Authority", "pakistan")
                                .toLowerCase().replaceAll("[.-]", "_");
                o = VoteResolver.Organization.valueOf(arg);
            } catch(IllegalArgumentException iae) {
                o = VoteResolver.Organization.guest;
                System.err.println("Unknown organization: "+this.org);
            }
            return o;
        }
        
        private String voterOrg = null;
        
        /**
         * Convenience function for returning the "VoteResult friendly" organization.
         * @return
         */
        public String voterOrg() {
            if(voterOrg==null) {
                voterOrg=voterInfo().getOrganization().name();
            }
            return voterOrg;
        }
        /**
         * Is this user an administrator 'over' this user?  Always true if admin, orif TC in same org. 
         * @param other
         * @return
         */
        public boolean isAdminFor(User other) {
            boolean adminOrRelevantTc = UserRegistry.userIsAdmin(this) || 
                        ( UserRegistry.userIsTC(this) && (other!=null) && this.org.equals(other.org)); 
            return adminOrRelevantTc;
        }
    }
        
    public static void printPasswordLink(WebContext ctx, String email, String password) {
        ctx.println("<a href='" + ctx.base() + "?email=" + email + "&amp;uid=" + password + "'>Login for " + 
            email + "</a>");
    }
    
    /**
     * The name of the user sql database
     */
    public static final String CLDR_USERS = "cldr_users";
    public static final String CLDR_INTEREST = "cldr_interest";
    
    /** 
     * Called by SM to create the reg
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @param isNew  true if should CREATE TABLEs
     */
    public static UserRegistry createRegistry(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain theSm) 
      throws SQLException
    {
        sm = theSm;
        UserRegistry reg = new UserRegistry(xlogger,ourConn);
        reg.setupDB();
//        logger.info("UserRegistry DB: created");
        return reg;
    }
    
    /**
     * Called by SM to shutdown
     */
    public void shutdownDB() throws SQLException {
        SurveyMain.closeDBConnection(conn);
    }

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException
    {
        String sql = null;
        try{
            synchronized(conn) {
    //            logger.info("UserRegistry DB: initializing...");
                boolean hadUserTable = sm.hasTable(conn,CLDR_USERS);
                if(!hadUserTable) {
                    Statement s = conn.createStatement();
                
                    sql = ("create table " + CLDR_USERS + "(id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                                                            "userlevel int not null, " +
                                                            "name "+sm.DB_SQL_UNICODE+" not null, " +
                                                            "email varchar(256) not null UNIQUE, " +
                                                            "org varchar(256) not null, " +
                                                            "password varchar(100) not null, " +
                                                            "audit varchar(1024) , " +
                                                            "locales varchar(1024) , " +
                                                            "prefs varchar(1024) , " +
                                                            "intlocs varchar(1024) , " + // added apr 2006: ALTER table CLDR_USERS ADD COLUMN intlocs VARCHAR(1024)
                                                            "lastlogin TIMESTAMP " + // added may 2006:  alter table CLDR_USERS ADD COLUMN lastlogin TIMESTAMP
                                                            (sm.db_Mysql?"":",primary key(id)")+
                                                                ")"); 
                    s.execute(sql);
                    sql=("INSERT INTO " + CLDR_USERS + "(userlevel,name,org,email,password) " +
                                                            "VALUES(" + ADMIN +"," + 
                                                            "'admin'," + 
                                                            "'SurveyTool'," +
                                                            "'admin@'," +
                                                            "'" + sm.vap +"')");
                    s.execute(sql);
                    sql = null;
                    logger.info("DB: added user Admin");
                    
                    s.close();
                    conn.commit();
                }
    
                boolean hadInterestTable = sm.hasTable(conn,CLDR_INTEREST);
                if(!hadInterestTable) {
                    Statement s = conn.createStatement();
                
                    sql=("create table " + CLDR_INTEREST + " (uid INT NOT NULL , " +
                                                            "forum  varchar(256) not null " +
                                                            ")"); 
                    s.execute(sql);
                    sql = "CREATE  INDEX " + CLDR_INTEREST + "_id_loc ON " + CLDR_INTEREST + " (uid) ";
                    s.execute(sql); 
                    sql = "CREATE  INDEX " + CLDR_INTEREST + "_id_for ON " + CLDR_INTEREST + " (forum) ";
                    s.execute(sql); 
                    logger.info("DB: created "+CLDR_INTEREST);
                    sql=null;
                    s.close();
                    conn.commit();
                }
                
                myinit(); // initialize the prepared statements
                
                if(!hadInterestTable) {
                    setupIntLocs();  // set up user -> interest table mapping
                }
    
            }
        } catch(SQLException se) {
            se.printStackTrace();
            System.err.println("SQL err: " + SurveyMain.unchainSqlException(se));
            System.err.println("Last SQL run: " + sql);
            throw se;
        }
            
    }
    
    /**
     * ID# of the user
     */
    static final int ADMIN_ID = 1;
    
    /**
     * special ID meaning 'all'
     */
    static final int ALL_ID = -1;
	
    /**
     * Migrate the user DB from the separate user DB to the main DB
     * @deprecated
     */
//	public void migrateFrom(Connection uConn) throws SQLException {
//		System.err.println("USER MIGRATE");
//		Statement s = conn.createStatement(); // 'new' side
//		int n = s.executeUpdate("DROP TABLE "+CLDR_USERS+"");
//		System.err.println("drop table: " + n + " rows from 'new' side table");
//		setupDB();
//		System.err.println("Reset table");
//		Statement oldS = uConn.createStatement(); // old side
//
//		ResultSet rs = oldS.executeQuery("select id from "+CLDR_USERS+" order by id desc");
//		if(rs.next()) {
//			n=rs.getInt(1);
//		} else { 
//			throw new RuntimeException("Err: couldn't count old users");
//		}
//		rs.close();
//
//		System.err.println("max userid: " + n);
//		
//		for(;n>0;n--) {
//			s.execute("INSERT INTO "+CLDR_USERS+" (userlevel,name,org,email,password) VALUES(999,'junk user"+n+"','_Delete','delete"+n+"@example.com','delete"+("delete"+n).hashCode()+"')");
//		}
//		conn.commit();
//		System.err.println("dummy users created.");
//		
//		PreparedStatement patchStmt = conn.prepareStatement("UPDATE " + CLDR_USERS + " set userlevel=?,name=?,email=?,org=?,password=?,locales=?,intlocs=?,lastlogin=?  " +
//                                                    " where id=?" );
//
//		Statement us = uConn.createStatement();
//		rs = us.executeQuery("SELECT userlevel,name,email,org,password,locales,intlocs,lastlogin,id from "+CLDR_USERS);
//		//								1         2   3    4     5       6      7        8       9
//		n=0;
//		while(rs.next()) {
//			n++;
//			patchStmt.setInt(1,		rs.getInt(1));
//			SurveyMain.setStringUTF8(patchStmt, 2, SurveyMain.getStringUTF8(rs, 2));
//			patchStmt.setString(3,	rs.getString(3));
//			patchStmt.setString(4,	rs.getString(4));
//			patchStmt.setString(5,  rs.getString(5));
//			patchStmt.setString(6,  rs.getString(6));
//			patchStmt.setString(7,  rs.getString(7));
//			patchStmt.setTimestamp(8, rs.getTimestamp(8));
//			patchStmt.setInt(9, rs.getInt(9));
//			
//			int j = patchStmt.executeUpdate();
//			System.err.println("n"+n+", id"+rs.getInt(9)+", e:"+rs.getString(3) + " -> updt. " + j);
//		}
//		conn.commit();
//		rs.close();
//		
//		System.err.println("Done.  Probably good to restart ST now.");
//	}
    
    public void importOldUsers(String dir) throws SQLException
    {
        int nUsers = 0;
        synchronized(conn) {
            try {
                logger.info("Importing old users...");
                  importStmt = conn.prepareStatement("INSERT  INTO " + CLDR_USERS +" (userlevel,password,email,org,name,audit) " +
                    " VALUES(999,?,?,?,?,'Imported')");       
                    
                OldUserRegistry our = new OldUserRegistry(dir);
                if(!our.read()) {
                    logger.severe("Couldn't import old user registry from " + dir);
                    return;
                }
                Hashtable emails = our.getEmails();
                Iterator e = emails.values().iterator();
                while(e.hasNext()) {
                    OldUserRegistry.User u = (OldUserRegistry.User)e.next();
                    importStmt.setString(1,u.id);
                    importStmt.setString(2,u.email);
                    importStmt.setString(3,u.sponsor);
                    SurveyMain.setStringUTF8(importStmt, 4, u.real); //    importStmt.setString(4,u.real);
                    importStmt.execute();
                    nUsers++;   
                }
                
            } finally {
                logger.info("Imported " + nUsers + " users.");
                if(importStmt != null) {
                    importStmt.close();
                    importStmt = null;
                }
                conn.commit();
            }
        }
    }
    
    private void myinit() throws SQLException {
     try {
        synchronized(conn) {
            insertStmt = conn.prepareStatement("INSERT INTO " + CLDR_USERS + "(userlevel,name,org,email,password,locales) " +
            "VALUES(?,?,?,?,?,?)" );
            queryStmt = conn.prepareStatement("SELECT id,name,userlevel,org,locales,intlocs,lastlogin from " + CLDR_USERS +" where email=? AND password=?",
                    ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            queryIdStmt = conn.prepareStatement("SELECT name,org,email,userlevel,intlocs,locales,lastlogin,password from " + CLDR_USERS +" where id=?",
                    ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            queryEmailStmt = conn.prepareStatement("SELECT id,name,userlevel,org,locales,intlocs,lastlogin from " + CLDR_USERS +" where email=?",
                    ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            touchStmt = conn.prepareStatement("UPDATE "+CLDR_USERS+" set lastlogin=CURRENT_TIMESTAMP where id=?");

            updateInfoEmailStmt = conn.prepareStatement("UPDATE "+CLDR_USERS+" set email=? WHERE id=? AND email=?");
            updateInfoNameStmt = conn.prepareStatement("UPDATE "+CLDR_USERS+" set name=? WHERE id=? AND email=?");
            updateInfoPasswordStmt = conn.prepareStatement("UPDATE "+CLDR_USERS+" set password=? WHERE id=? AND email=?");

            removeIntLoc = conn.prepareStatement("DELETE FROM "+CLDR_INTEREST+" WHERE uid=?");
            updateIntLoc = conn.prepareStatement("INSERT INTO " + CLDR_INTEREST + " (uid,forum) VALUES(?,?)");
        }
      }finally{
        if(queryStmt == null) {
            logger.severe("queryStmt failed to initialize");
        }
        if(queryIdStmt == null) {
            logger.severe("queryIdStmt failed to initialize");
        }
        if(insertStmt == null) {
            logger.severe("insertStmt failed to initialize");
        }
        if(touchStmt == null) {
            logger.severe("touchStmt failed to initialize");
        }
        if(updateInfoEmailStmt == null) {
            logger.severe("updateInfoStmt failed to initialize");
        }
        if(updateInfoNameStmt == null) {
            logger.severe("updateInfoStmt failed to initialize");
        }
      }
    }
    
    /**
     * info = name/email/org
     * immutable info, keep it in a separate list for quick lookup.
     */
    public final static int CHUNKSIZE = 128;
    int arraySize = 0;
    UserRegistry.User infoArray[] = new UserRegistry.User[arraySize];
    
    /**
     * Mark user as modified
     * @param id
     */
    void userModified(int id) {
        synchronized(infoArray) {
            try {
                infoArray[id] = null;
            } catch(IndexOutOfBoundsException ioob) {
                // nothing to do
            }
        }
        userModified(); // do this if any users are modified
    }

    
    /**
     * Mark the UserRegistry as changed, purging the VoterInfo map
     * @see #getVoterToInfo()
     */
    private void userModified() {
        voterInfo = null;
    }

    public UserRegistry.User getInfo(int id) {
//    System.err.println("Fetching info for id " + id);
        synchronized(infoArray) {
            User ret = null;
            try {
//    System.err.println("attempting array lookup for id " + id);
                //ret = (User)infoArray.get(id);
                ret = infoArray[id];
            } catch(IndexOutOfBoundsException ioob) {
//    System.err.println("Index out of bounds for id " + id + " - " + ioob);
                ret = null; // not found
            }
            
            if(ret == null) synchronized(conn) {
//    System.err.println("go fish for id " + id);
//          queryIdStmt = conn.prepareStatement("SELECT name,org,email from " + CLDR_USERS +" where id=?");
                ResultSet rs = null;
                try{ 
                    PreparedStatement pstmt = null;
                    pstmt = queryIdStmt;
                    pstmt.setInt(1,id);
                    // First, try to query it back from the DB.
                    rs = pstmt.executeQuery();                
                    if(!rs.next()) {
//                        System.err.println("Unknown user#:" + id);
                        return null;
                    }
                    User u = new UserRegistry.User();                    
                    // from params:
                    u.id = id;
                    u.name = SurveyMain.getStringUTF8(rs, 1);// rs.getString(1);
                    u.org = rs.getString(2);
                    u.email = rs.getString(3);
                    u.userlevel = rs.getInt(4);
                    u.intlocs = rs.getString(5);
                    u.locales = rs.getString(6);
                    u.last_connect = rs.getTimestamp(7);
                    //          queryIdStmt = conn.prepareStatement("SELECT name,org,email,userlevel,intlocs,lastlogin,password from " + CLDR_USERS +" where id=?",

//                    System.err.println("SQL Loaded info for U#"+u.id + " - "+u.name +"/"+u.org+"/"+u.email);
                    ret = u; // let it finish..

                    if(id >= arraySize) {
                        int newchunk = (((id+1)/CHUNKSIZE)+1)*CHUNKSIZE;
//                        System.err.println("UR: userInfo resize from " + infoArray.length + " to " + newchunk);
                        infoArray = new UserRegistry.User[newchunk];
                        arraySize = newchunk;
                    }
                    infoArray[id]=u;
                    // good so far..
                    if(rs.next()) {
                        // dup returned!
                        throw new InternalError("Dup user id # " + id);
                    }
                } catch (SQLException se) {
                    logger.log(java.util.logging.Level.SEVERE, "UserRegistry: SQL error trying to get #" + id + " - " + SurveyMain.unchainSqlException(se),se);
                    return ret;
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE, "UserRegistry: some error trying to get #" + id,t);
                    return ret;
                } finally {
                    // close out the RS
                    try {
                        if(rs!=null) {
                            rs.close();
                        }
                    } catch(SQLException se) {
                        /*logger.severe*/System.err.println(/*java.util.logging.Level.SEVERE,*/ "UserRegistry: SQL error trying to close resultset for: #" + id + " - " + SurveyMain.unchainSqlException(se)/*,se*/);
                    }
                } // end try
            }
///*srl*/    if(ret==null) { System.err.println("returning NULL for " + id); } else  { User u = ret; System.err.println("Returned info for U#"+u.id + " - "+u.name +"/"+u.org+"/"+u.email); }
            return ret;
        } // end synch array
    }
    
    public  UserRegistry.User get(String pass, String email, String ip) {
        if((email == null)||(email.length()<=0)) {
            return null; // nothing to do
        }
        if((pass == null)||(pass.length()<=0)) {
            return null; // nothing to do
        }
        ResultSet rs = null;
        synchronized(conn) {
            try{ 
                PreparedStatement pstmt = null;
                if(pass != null) {
//                    logger.info("Looking up " + email + " : " + pass);
                    pstmt = queryStmt;
                    pstmt.setString(1,email);
                    pstmt.setString(2,pass);
                } else {
//                    logger.info("Looking up " + email);
                    pstmt = queryEmailStmt;
                    pstmt.setString(1,email);
                }
                // First, try to query it back from the DB.
                rs = pstmt.executeQuery();                
                if(!rs.next()) {
                    logger.info("Unknown user or bad login: " + email + " @ " + ip);
                    return null;
                }
                User u = new UserRegistry.User();
                
                // from params:
                u.password = pass;
                u.email = email;
                // from db:   (id,name,userlevel,org,locales)
                u.id = rs.getInt(1);
                u.name = SurveyMain.getStringUTF8(rs, 2);//rs.getString(2);
                u.userlevel = rs.getInt(3);
                u.org = rs.getString(4);
                u.locales = rs.getString(5);
                u.intlocs = rs.getString(6);
                u.last_connect = rs.getTimestamp(7);
                
                // good so far..
                
                if(rs.next()) {
                    // dup returned!
                    logger.severe("Duplicate user for " + email + " - ids " + u.id + " and " + rs.getInt(1));
                    return null;
                }
                if(!ip.startsWith("RSS@") && !ip.equals("INTERNAL")) {
                    logger.info("Login: " + email + " @ " + ip);
                    if(!FOR_ADDING.equals(ip)) {
                        touchStmt.setInt(1, u.id);
                        touchStmt.executeUpdate();
                        conn.commit();
                    }
                }
                
                return u;
            } catch (SQLException se) {
                logger.log(java.util.logging.Level.SEVERE, "UserRegistry: SQL error trying to get " + email + " - " + SurveyMain.unchainSqlException(se),se);
                return null;
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.SEVERE, "UserRegistry: some error trying to get " + email,t);
                return null;
            } finally {
                // close out the RS
                try {
                    if(rs!=null) {
                        rs.close();
                    }
                } catch(SQLException se) {
                    logger.log(java.util.logging.Level.SEVERE, "UserRegistry: SQL error trying to close resultset for: " + email + " - " + SurveyMain.unchainSqlException(se),se);
                }
            } // end try
        } // end synch(conn)
    } // end get

    public UserRegistry.User get(String email) {
        return get(null,email,"(lookup)");
    }
    public UserRegistry.User getEmptyUser() {
        User u = new User();
        u.id = -1;
        u.name = "UNKNOWN";
        u.email = "UN@KNOWN.example.com";
        u.org = "NONE"; 
        u.password = null;
        u.locales="";
        
       return u;   
    }
    
    Connection conn = null;
    static SurveyMain sm = null; // static for static checking of defaultContent..

    private UserRegistry(java.util.logging.Logger xlogger, Connection ourConn) {
        logger = xlogger;
        conn = ourConn;
    }

    // ------- special things for "list" mode:
    
    public java.sql.ResultSet list(String organization) throws SQLException {
        ResultSet rs = null;
        Statement s = null;
        final String ORDER = " ORDER BY org,userlevel,name ";
        synchronized(conn) {
//            try {
                s = conn.createStatement();
                if(organization == null) {
                    rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales,intlocs,lastlogin FROM " + CLDR_USERS + ORDER);
                } else {
                    rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales,intlocs,lastlogin FROM " + CLDR_USERS + " WHERE org='" + organization + "'" + ORDER);
                }
//            } finally  {
//                s.close();
//            }
        }
        
        return rs;
    }
    public java.sql.ResultSet listPass() throws SQLException {
        ResultSet rs = null;
        Statement s = null;
        final String ORDER = " ORDER BY id ";
        synchronized(conn) {
//            try {
                s = conn.createStatement();
                rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales,intlocs, password FROM " + CLDR_USERS + ORDER);
//            } finally  {
//                s.close();
//            }
        }
        
        return rs;
    }
    
    void setupIntLocs() throws SQLException {
        ResultSet rs = list(null);
        ElapsedTimer et = new ElapsedTimer();
        int count=0;
        while(rs.next()) {
            int user = rs.getInt(1);
//            String who = rs.getString(4);
            
            updateIntLocs(user);
            count++;
        }
        System.err.println("update:" + count + " user's locales updated " + et);
    }
    /**
     * assumes caller has a lock on conn
     */
    String updateIntLocs(int user) throws SQLException {
        return updateIntLocs(user, true);
    }
    
    static String normalizeLocaleList(String list) {
        list = list.trim();
        if(list.length()>0) {
            if(list.equals("none")) {
                return "none";
            }
            Set<String> s = new TreeSet<String>();
            for(String l : UserRegistry.tokenizeLocale(list) ) {
                String forum = new ULocale(l).getLanguage();
                s.add(forum);
            }
            list = null;
            for(String forum : s) {
                if(list == null) {
                    list = forum;
                } else {
                    list = list+" "+forum;
                }
            }
        }
        return list;
    }
    
    /**
     * assumes caller has a lock on conn
     */
    String updateIntLocs(int id, boolean doCommit) throws SQLException {
        // do something
        User user = getInfo(id);
        if(user==null) {
            return "";
        }
        
        removeIntLoc.setInt(1,id);
        int n = removeIntLoc.executeUpdate();
        //System.err.println(id+":"+user.email+" - removed intlocs " + n);
        
        n = 0;
        
        String[] il = user.getInterestList();
        if(il != null ) {
            updateIntLoc.setInt(1,id);
            Set<String> s = new HashSet<String>();
            for(String l : il ) {
                //System.err.println(" << " + l);
                String forum = new ULocale(l).getLanguage();
                s.add(forum);
            }
            for(String forum : s) {
                //System.err.println(" >> " + forum);
                updateIntLoc.setString(2,forum);
                n += updateIntLoc.executeUpdate();
            }
        }
        
        //System.err.println(id+":"+user.email+" - updated intlocs " + n);
        
        if(doCommit) {
            conn.commit();
        }
        return "";
    }

    String setUserLevel(WebContext ctx, int theirId, String theirEmail, int newLevel) {
        if((newLevel < ctx.session.user.userlevel) || (ctx.session.user.userlevel > TC)) {
            return ("[Permission Denied]");
        }

        String orgConstraint = null;
        String msg = "";
        if(ctx.session.user.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + ctx.session.user.org + "' ";
        }
        synchronized(conn) {
            try {
                Statement s = conn.createStatement();
                String theSql = "UPDATE " + CLDR_USERS + " SET userlevel=" + newLevel + 
                    " WHERE id=" + theirId + " AND email='" + theirEmail + "' "  + orgConstraint;
      //           msg = msg + " (<br /><pre> " + theSql + " </pre><br />) ";
                logger.info("Attempt user update by " + ctx.session.user.email + ": " + theSql);
                int n = s.executeUpdate(theSql);
                conn.commit();
                userModified(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [user level set]";
                    msg = msg + updateIntLocs(theirId);
                }
            } catch (SQLException se) {
                msg = msg + " exception: " + SurveyMain.unchainSqlException(se);
            } catch (Throwable t) {
                msg = msg + " exception: " + t.toString();
            } finally  {
              //  s.close();
            }
        }
        
        return msg;
    }

    String setLocales(WebContext ctx, int theirId, String theirEmail, String newLocales) {
        return setLocales(ctx, theirId, theirEmail, newLocales, false);
    }
    
    String setLocales(WebContext ctx, int theirId, String theirEmail, String newLocales, boolean intLocs) {
        if(!intLocs && ctx.session.user.userlevel > TC) { // Note- we dont' check that a TC isn't modifying an Admin's locale. 
            return ("[Permission Denied]");
        }
        
        newLocales = normalizeLocaleList(newLocales);

        String orgConstraint = null;
        String msg = "";
        if(ctx.session.user.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + ctx.session.user.org + "' ";
        }
        synchronized(conn) {
            try {
                String theSql = "UPDATE " + CLDR_USERS + " SET "+
                    (intLocs?"intlocs":"locales") + "=? WHERE id=" + theirId + " AND email='" + theirEmail + "' "  + orgConstraint;
                PreparedStatement ps = conn.prepareStatement(theSql);
      //           msg = msg + " (<br /><pre> " + theSql + " </pre><br />) ";
                logger.info("Attempt user locales update by " + ctx.session.user.email + ": " + theSql + " - " + newLocales);
                ps.setString(1, newLocales);
                int n = ps.executeUpdate();
                conn.commit();
                userModified(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [locales set]";
                    msg = msg + updateIntLocs(theirId);
                    /*if(intLocs) { 
                        return updateIntLocs(theirId);
                    }*/
                }
            } catch (SQLException se) {
                msg = msg + " exception: " + SurveyMain.unchainSqlException(se);
            } catch (Throwable t) {
                msg = msg + " exception: " + t.toString();
            } finally  {
              //  s.close();
            }
        }
        
        return msg;
    }


    String delete(WebContext ctx, int theirId, String theirEmail) {
        if(ctx.session.user.userlevel > TC) {
            return ("[Permission Denied]");
        }

        String orgConstraint = null; // keep org constraint in place
        String msg = "";
        if(ctx.session.user.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + ctx.session.user.org + "' ";
        }
        synchronized(conn) {
            try {
                Statement s = conn.createStatement();
                String theSql = "DELETE FROM " + CLDR_USERS + 
                    " WHERE id=" + theirId + " AND email='" + theirEmail + "' "  + orgConstraint;
//                 msg = msg + " (<br /><pre> " + theSql + " </pre><br />) ";
                logger.info("Attempt user DELETE by " + ctx.session.user.email + ": " + theSql);
                int n = s.executeUpdate(theSql);
                conn.commit();
                userModified(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were removed!] ";
                    logger.severe("Error: 0 users removed.");
                } else if(n != 1) {
                    msg = msg + " [Error in removing users!] ";
                    logger.severe("Error: " + n + " records removed!");
                } else {
                    msg = msg + " [removed OK]";
                }
            } catch (SQLException se) {
                msg = msg + " exception: " + SurveyMain.unchainSqlException(se);
            } catch (Throwable t) {
                msg = msg + " exception: " + t.toString();
            } finally  {
              //  s.close();
            }
        }
        
        return msg;
    }
    
    public enum InfoType { INFO_EMAIL, INFO_NAME, INFO_PASSWORD };
    
    String updateInfo(WebContext ctx, int theirId, String theirEmail, InfoType type, String value) {
        if(ctx.session.user.userlevel > TC) {
            return ("[Permission Denied]");
        }

        String msg = "";
        synchronized(conn) {
            try {
                PreparedStatement updateInfoStmt = null;
                
                //updateInfoStmt = conn.prepareStatement("UPDATE CLDR_USERS set ?=? WHERE id=? AND email=?");
                switch(type) {
                    case INFO_EMAIL: 
                        updateInfoStmt = updateInfoEmailStmt;
                        value = value.toLowerCase();
                        break;
                    case INFO_NAME:
                        updateInfoStmt = updateInfoNameStmt;
                        break;
                    case INFO_PASSWORD:
                        updateInfoStmt = updateInfoPasswordStmt;
                        break;
                    default:
                        return("[unknown type: " + type.toString() +"]");
                }
                if(type==UserRegistry.InfoType.INFO_NAME) { // unicode treatment
                    SurveyMain.setStringUTF8(updateInfoStmt, 1, value);
                } else {
                    updateInfoStmt.setString(1, value);
                }
                updateInfoStmt.setInt(2,theirId);
                updateInfoStmt.setString(3,theirEmail);
                
                logger.info("Attempt user UPDATE by " + ctx.session.user.email + ": " + type.toString() + " = " + value);
                int n = updateInfoStmt.executeUpdate();
                conn.commit();
                userModified(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 users updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updated users!] ";
                    logger.severe("Error: " + n + " updated removed!");
                } else {
                    msg = msg + " [updated OK]";
                }
            } catch (SQLException se) {
                msg = msg + " exception: " + SurveyMain.unchainSqlException(se);
            } catch (Throwable t) {
                msg = msg + " exception: " + t.toString();
            } finally  {
              //  s.close();
            }
        }
        
        return msg;
    }
    

    public String getPassword(WebContext ctx, int theirId)  {
        ResultSet rs = null;
        Statement s = null;
        String result = null;
        synchronized(conn) {
            logger.info("UR: Attempt getPassword by " + ctx.session.user.email + ": of #" + theirId);
            try {
                s = conn.createStatement();
                rs = s.executeQuery("SELECT password FROM " + CLDR_USERS + " WHERE id=" + theirId);
                if(!rs.next()) {
                    ctx.println("Couldn't find user.");
                    return null;
                }
                result = rs.getString(1);
                if(rs.next()) {
                    ctx.println("Matched duplicate user (?)");
                    return null;
                }                
            } catch (SQLException se) {
                logger.severe("UR:  exception: " + SurveyMain.unchainSqlException(se));
                ctx.println(" An error occured: " + SurveyMain.unchainSqlException(se));
            } catch (Throwable t) {
                logger.severe("UR:  exception: " + t.toString());
                ctx.println(" An error occured: " + t.toString());
            } finally {
            //    if(rs != null ) rs.close();
            //    if(s != null) s.close();
            }
        }
        
        return result;
    }

    public static String makePassword(String email) {
        return CookieSession.newId(false).substring(0,9);
//        return  CookieSession.cheapEncode((System.currentTimeMillis()*100) + SurveyMain.pages) + "x" + 
//            CookieSession.cheapEncode(email.hashCode() * SurveyMain.vap.hashCode());
    }

    public User newUser(WebContext ctx, User u) {
        if(ctx.session.user.userlevel > TC) {
            return null;
        }
        // prepare quotes 
        u.email = u.email.replace('\'', '_').toLowerCase();
        u.org = u.org.replace('\'', '_');
        u.name = u.name.replace('\'', '_');
        u.locales = u.locales.replace('\'', '_');

        synchronized(conn) {
            try {
                logger.info("UR: Attempt newuser by " + ctx.session.user.email + ": of " + u.email + " @ " + ctx.userIP());
                insertStmt.setInt(1, u.userlevel);
                SurveyMain.setStringUTF8(insertStmt, 2, u.name); //insertStmt.setString(2, u.name);
                insertStmt.setString(3, u.org);
                insertStmt.setString(4, u.email);
                insertStmt.setString(5, u.password);
                insertStmt.setString(6, normalizeLocaleList(u.locales));
                if(!insertStmt.execute()) {
                    logger.info("Added.");
                    conn.commit();
                    ctx.println("<p>Added user.<p>");
                    User newu =  get(u.password, u.email,FOR_ADDING); // throw away old user
                    updateIntLocs(newu.id);
                    return newu;
                } else {
                    ctx.println("Couldn't add user.");
                    conn.commit();
                    return null;
                }
            } catch (SQLException se) {
                logger.severe("UR: Adding: exception: " + SurveyMain.unchainSqlException(se));
            } catch (Throwable t) {
                logger.severe("UR: Adding: exception: " + t.toString());
            } finally  {
                userModified(); // new user
              //  s.close();
            }
        }
        
        return null;
    }
    
    // All of the userlevel policy is concentrated here, or in above functions (search for 'userlevel')
    
    // * user types
    static final boolean userIsAdmin(User u) {
        return (u!=null)&&(u.userlevel <= UserRegistry.ADMIN);
    }
    static final boolean userIsTC(User u) {
        return (u!=null)&&(u.userlevel <= UserRegistry.TC);
    }
    static final boolean userIsExpert(User u) {
        return (u!=null)&&(u.userlevel <= UserRegistry.EXPERT);
    }
    static final boolean userIsVetter(User u) {
        return (u!=null)&&(u.userlevel <= UserRegistry.VETTER);
    }
    static final boolean userIsStreet(User u) {
        return (u!=null)&&(u.userlevel <= UserRegistry.STREET);
    }
    static final boolean userIsLocked(User u) {
        return (u!=null)&&(u.userlevel == UserRegistry.LOCKED);
    }
    // * user rights
    /** can create a user in a different organization? */
    static final boolean userCreateOtherOrgs(User u) {
        return userIsAdmin(u);
    }
    /** What level can the new user be, given requested? */
    static final int userCanCreateUserOfLevel(User u, int requestedLevel) {
        if(requestedLevel < 0) {
            requestedLevel = 0;
        }
        if(requestedLevel < u.userlevel) { // pin to creator
            requestedLevel = u.userlevel;
        }
        return requestedLevel;
    }
    /** Can the user modify anyone's level? */
    static final boolean userCanModifyUsers(User u) {
        return userIsTC(u);
    }
    static final boolean userCanEmailUsers(User u) {
        return userIsTC(u);
    }
    /** can the user modify this particular user? */
    static final boolean userCanModifyUser(User u, int theirId, int theirLevel) {
        return (  userCanModifyUsers(u) &&
                 (theirId != ADMIN_ID) &&
                 (theirId != u.id) &&
                 (theirLevel >= u.userlevel) );
    }
    static final boolean userCanDeleteUser(User u, int theirId, int theirLevel) {
        return (userCanModifyUser(u,theirId,theirLevel) &&
                theirLevel > u.userlevel); // must be at a lower level
    }
    static final boolean userCanChangeLevel(User u, int theirLevel, int newLevel) {
        int ourLevel = u.userlevel;
        return (userCanModifyUser(u, ALL_ID, theirLevel) &&
            (newLevel >= ourLevel) && // new level is equal to or greater than our level
           (newLevel != theirLevel) ); // not existing level 
    }
    static final boolean userCanDoList(User u) {
        return (userIsVetter(u));
    }
    static final boolean userCanCreateUsers(User u) {
        return (userIsTC(u));
    }
    static final boolean userCanSubmit(User u) {
		if(SurveyMain.isPhaseReadonly()) return false;
        return((u!=null) && userIsStreet(u));
    }
    
    // TODO: move to CLDRLocale
    static final boolean localeMatchesLocale(CLDRLocale smallLocale, CLDRLocale bigLocale) {
        if(bigLocale.toString().startsWith(smallLocale.toString())) {
            int blen = bigLocale.toString().length();
            int slen = smallLocale.toString().length();
            
            if(blen==slen) {
                return true;  // exact match.   'ro' matches 'ro'
            } else if(!java.lang.Character.isLetter(bigLocale.toString().charAt(slen))) {
                return true; // next char is NOT a letter. 'ro' matches 'ro_...'
            } else {
                return false; // next char IS a letter.  'ro' DOES NOT MATCH 'root'
            }
        } else {
            return false; // no substring (common case)
        }
    }
    
    static final boolean userCanModifyLocale(CLDRLocale uLocale, CLDRLocale aliasTarget) {
        if(SurveyMain.isPhaseReadonly()) return false;
        return localeMatchesLocale(uLocale, aliasTarget);
    }

    static boolean localeMatchesLocaleList(String localeArray[], CLDRLocale locale) {
        return localeMatchesLocaleList(stringArrayToLocaleArray(localeArray),locale);
    }
    static boolean localeMatchesLocaleList(CLDRLocale localeArray[], CLDRLocale locale)
    {
        for(int i=0;i<localeArray.length;i++) {
            if(localeMatchesLocale(localeArray[i],locale)) {
                return true;
            }
        }
        return false;
    }

    static boolean localeMatchesLocaleList(String localeList, CLDRLocale locale)
    {
        String localeArray[] = tokenizeLocale(localeList);
        return localeMatchesLocaleList(localeArray, locale);
    }
        
    
    static final boolean userCanModifyLocale(CLDRLocale localeArray[], CLDRLocale locale) {
		if(SurveyMain.isPhaseReadonly()) return false;
        if(localeArray.length == 0) {
            return true; // all 
        }
        
        return localeMatchesLocaleList(localeArray, locale);
    }
    
    static final boolean userCanModifyLocale(User u, CLDRLocale locale) {
        if(u==null) return false; // no user, no dice

        if(!userIsStreet(u)) return false; // at least street level
        if(SurveyMain.isPhaseReadonly()) return false; // readonly = locked for ALL
        if((sm.isLocaleAliased(locale)!=null) ||
            sm.supplemental.defaultContentToParent(locale.toString())!=null) return false; // it's a defaultcontent locale or a pure alias.

        if(userIsAdmin(u)) return true; // Admin can modify all
        if(userIsTC(u)) return true; // TC can modify all
        if((SurveyMain.phase() == SurveyMain.Phase.VETTING_CLOSED)) {
//            if(u.userIsSpecialForCLDR15(locale)) {
//                return true;
//            } else {
//                return false;
//            }
        }
        if(userIsTC(u)) return true; // TC can modify all
        if(SurveyMain.isPhaseClosed()) return false;
        if(SurveyMain.isPhaseSubmit() && !userIsStreet(u)) return false;
        if(SurveyMain.isPhaseVetting() && !userIsStreet (u)) return false;
        if(locale.getLanguage().equals("und")) {  // all user accounts can write to und.
            return true;
        }
//        if(SurveyMain.phaseVetting && !userIsStreet(u)) return false;
        if((u.locales == null) && userIsExpert(u)) return true; // empty = ALL
        String localeArray[] = tokenizeLocale(u.locales);
        return userCanModifyLocale(localeArray,locale);
    }

    private static boolean userCanModifyLocale(String[] localeArray, CLDRLocale locale) {
        return userCanModifyLocale(stringArrayToLocaleArray(localeArray), locale);
    }
    private static CLDRLocale[] stringArrayToLocaleArray(String[] localeArray) {
        CLDRLocale arr[] = new CLDRLocale[localeArray.length];
        for(int j=0;j<localeArray.length;j++) {
            arr[j]=CLDRLocale.getInstance(localeArray[j]);
        }
        return arr;
    }
    static final boolean userCanSubmitLocale(User u, CLDRLocale locale) {
		return userCanSubmitLocaleWhenDisputed(u, locale, false);
    }
	

    static final boolean userCanSubmitLocaleWhenDisputed(User u, CLDRLocale locale, boolean disputed) {
		if(SurveyMain.isPhaseReadonly()) return false;
        if(u==null) return false; // no user, no dice
        if(userIsTC(u)) return true; // TC can modify all
        if((SurveyMain.phase() == SurveyMain.Phase.VETTING_CLOSED)) {
            if(u.userIsSpecialForCLDR15(locale)) {
                return true;
            } else {
                return false;
            }
        }
        if(SurveyMain.isPhaseClosed()) return false;
		if(!u.userIsSpecialForCLDR15(locale) && SurveyMain.isPhaseVetting() && !disputed && !userIsExpert(u)) return false; // only expert can submit new data.
        return userCanModifyLocale(u,locale);
    }

    static final boolean userCanSubmitAnyLocale(User u) {
		if(SurveyMain.isPhaseReadonly()) return false;
        if(u==null) return false; // no user, no dice
        if(userIsTC(u)) return true; // TC can modify all
        if((SurveyMain.phase() == SurveyMain.Phase.VETTING_CLOSED)) {
//            if(u.userIsSpecialForCLDR15("be")) {
//                return true;
//            }
        }
        if(SurveyMain.isPhaseClosed()) return false;
        if(SurveyMain.isPhaseVetting() && !userIsExpert(u)) return false; // only expert can submit new data.
        return userCanSubmit(u);
    }

    static final boolean userCanVetLocale(User u, CLDRLocale locale) {
		if(SurveyMain.isPhaseReadonly()) return false;
        if((SurveyMain.phase() == SurveyMain.Phase.VETTING_CLOSED) && u.userIsSpecialForCLDR15(locale)) {
            return true;
        }
        if(userIsTC(u) ) return true; // TC can modify all
        if(SurveyMain.isPhaseClosed()) return false;
        return userCanModifyLocale(u,locale);
    }
    
    static final String LOCALE_PATTERN = "[, \t\u00a0\\s]+"; // whitespace
    
    static String[] tokenizeLocale(String localeList) {
        if((localeList == null)||((localeList=localeList.trim()).length()==0)) {
//            System.err.println("TKL: null input");
            return new String[0];
        }
        return localeList.trim().split(LOCALE_PATTERN);
    }
     
    /**
     * take a locale string and convert it to HTML. 
     */
    static String prettyPrintLocale(String localeList) {
//        System.err.println("TKL: ppl - " + localeList);
        String[] localeArray = tokenizeLocale(localeList);
        String ret = "";
        if((localeList == null) || (localeArray.length == 0)) {
//            System.err.println("TKL: null output");
            ret = ("<i>all locales</i>");
        } else {
            for(int i=0;i<localeArray.length;i++) {
                ret = ret + " <tt class='codebox' title='"+new ULocale(localeArray[i]).getDisplayName()+"'>"+localeArray[i]+"</tt> ";
            }
        }
//        return ret + " [" + localeList + "]";
        return ret;
    }
    
    Set<User> specialUsers = null;

    public Set<User> getSpecialUsers() {
        return getSpecialUsers(false);
    }
    
    public synchronized Set<User> getSpecialUsers(boolean reread) {
        if(specialUsers == null) {
            reread = true;
        }
        if(reread == true) {
            doReadSpecialUsers();
        }
        return specialUsers;
    }
    
    private synchronized boolean doReadSpecialUsers() {
        String externalErrorName = sm.cldrHome + "/" + "specialusers.txt";

//        long now = System.currentTimeMillis();
//        
//        if((now-externalErrorLastCheck) < 8000) {
//            //System.err.println("Not rechecking errfile- only been " + (now-externalErrorLastCheck) + " ms");
//            if(externalErrorSet != null) {
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        externalErrorLastCheck = now;
        
        try {
            File extFile = new File(externalErrorName);
            
            if(!extFile.isFile() && !extFile.canRead()) {
                System.err.println("Can't read special user file: " + externalErrorName);
               // externalErrorFailed = true;
                return false;
            }
            
//            long newMod = extFile.lastModified();
//            
//            if(newMod == externalErrorLastMod) {
//                //System.err.println("** e.e. file did not change");
//                return true;
//            }
            
            // ok, now read it
            BufferedReader in
               = new BufferedReader(new FileReader(extFile));
            String line;
            int lines=0;
            Set<User> newSet = new HashSet<User>();
            System.err.println("* Reading special user file: " + externalErrorName);
            while ((line = in.readLine())!=null) {
                lines++;
                line = line.trim();
                if((line.length()<=0) ||
                  (line.charAt(0)=='#')) {
                    continue;
                }
                try {
                    int theirId = new Integer(line).intValue();
                    User u = getInfo(theirId);
                    if(u == null) {
                        System.err.println("Could not find user: " + line);
                        continue;
                    }
                    newSet.add(u);
                    System.err.println("*+ User: " + u.toString());

//                    String[] result = line.split("\t");
//                    String loc = result[0].split(";")[0];
//                    String what = result[1];
//                    String val = result[2];
//                    
//                    Set<Integer> aSet = newSet.get(loc);
//                    if(aSet == null) {
//                        aSet = new HashSet<Integer>();
//                        newSet.put(loc, aSet);
//                    }
//                    
//                    if(what.equals("path:")) {
//                        aSet.add(xpt.getByXpath(val));
//                    } else if(what.equals("count:")) {
//                        int theirCount = new Integer(val).intValue();
//                        if(theirCount != aSet.size()) {
//                            System.err.println(loc + " - count says " + val + ", we got " + aSet.size());
//                        }
//                    } else {
//                        throw new IllegalArgumentException("Unknown parameter: " + what);
//                    }
                } catch(Throwable t) {
                    System.err.println("** " + externalErrorName +":"+ lines + " -  " + t.toString());
                    //externalErrorFailed = true;
                    t.printStackTrace();
                    return false;  
                }
            }
            System.err.println(externalErrorName + " - " + lines + " and " + newSet.size() + " users loaded.");
            
//            externalErrorSet = newSet;
//            externalErrorLastMod = newMod;
//            externalErrorFailed = false;
            specialUsers = newSet;
            return true;
        } catch(IOException ioe) {
            System.err.println("Reading externalErrorFile: "  + "specialusers.txt - " + ioe.toString());
            ioe.printStackTrace();
            //externalErrorFailed = true;
            return false;
        }
    }
    
    public VoterInfo getVoterToInfo(int userid) {
        return getVoterToInfo().get(userid);
    }
    
    // Interface for VoteResolver interface
    /**
     * Fetch the user map in VoterInfo format.
     * @see #userModified()
     */
    public synchronized Map<Integer, VoterInfo> getVoterToInfo() {
        if(voterInfo == null) {
            Map<Integer, VoterInfo> map = new TreeMap<Integer, VoterInfo>();
            
            ResultSet rs = null;
            try {
                rs = list(null);
                // id,userlevel,name,email,org,locales,intlocs,lastlogin    
                while(rs.next()){
                    // We don't go through the cache, because not all users may be loaded.
                    
                    User u = new UserRegistry.User();
                    // from params:
                    u.id = rs.getInt(1);
                    u.userlevel = rs.getInt(2);
                    u.name = SurveyMain.getStringUTF8(rs, 3);
                    u.email = rs.getString(4);
                    u.org = rs.getString(5);
                    u.locales = rs.getString(6);
                    u.intlocs = rs.getString(7);
                    u.last_connect = rs.getTimestamp(8);
                    
                    // now, map it to a UserInfo
                    VoterInfo v = u.createVoterInfo();
                    
                    map.put(u.id, v);
                }
                voterInfo = map;
            } catch (SQLException se) {
                logger.log(java.util.logging.Level.SEVERE, "UserRegistry: SQL error trying to  update VoterInfo - " + SurveyMain.unchainSqlException(se),se);
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.SEVERE, "UserRegistry: some error trying to update VoterInfo - "  + t.toString(),t);
            } finally {
                // close out the RS
                try {
                    if(rs!=null) {
                        rs.close();
                    }
                } catch(SQLException se) {
                    /*logger.severe*/System.err.println(/*java.util.logging.Level.SEVERE,*/ "UserRegistry: SQL error trying to close resultset for: VI "  + " - " + SurveyMain.unchainSqlException(se)/*,se*/);
                }
            } // end try
        }
        return voterInfo;
    }
    
    /**
     * VoterInfo map
     */
    private Map<Integer, VoterInfo> voterInfo = null;
}
