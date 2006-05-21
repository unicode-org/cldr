//
//  UserRegistry.java
//  fourjay
//
//  Created by Steven R. Loomis on 14/10/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import com.ibm.icu.util.ULocale;


public class UserRegistry {
    private static java.util.logging.Logger logger;
    // user levels
    public static final int ADMIN   = 0;
    public static final int TC      = 1;
    public static final int EXPERT  = 3;
    public static final int VETTER  = 5;
    public static final int STREET  = 10;
    public static final int LOCKED  = 999;
    
    public static final String FOR_ADDING= "(for adding)";
    public static final int ALL_LEVELS[] = { // for UI presentation
        ADMIN, TC, EXPERT, VETTER, STREET, LOCKED };
    

    public static String levelToStr(WebContext ctx, int level) {
        return level + ": (" + levelAsStr(level) + ")";
    }
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
    PreparedStatement touchStmt = null;
    
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
        public void printPasswordLink(WebContext ctx) {
            UserRegistry.printPasswordLink(ctx, email, password);
        }
        public String toString() {
            return email + "("+org+")-" + levelAsStr(userlevel)+"#"+userlevel;
        }
        public int hashCode() { 
            return id;
        }
    }
        
    public static void printPasswordLink(WebContext ctx, String email, String password) {
        ctx.println("<a href='" + ctx.base() + "?email=" + email + "&amp;uid=" + password + "'>Login for " + 
            email + "</a>");
    }
    
    public static final String CLDR_USERS = "cldr_users";
    
    /** 
     * Called by SM to create the reg
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @param isNew  true if should CREATE TABLEs
     */
    public static UserRegistry createRegistry(java.util.logging.Logger xlogger, Connection ourConn, boolean isNew) 
      throws SQLException
    {
        UserRegistry reg = new UserRegistry(xlogger,ourConn);
        if(isNew) {
            reg.setupDB();
        }
        reg.myinit();
//        logger.info("UserRegistry DB: created");
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
        synchronized(conn) {
//            logger.info("UserRegistry DB: initializing...");
            Statement s = conn.createStatement();
            s.execute("create table " + CLDR_USERS + "(id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "userlevel int not null, " +
                                                    "name varchar(256) not null, " +
                                                    "email varchar(256) not null UNIQUE, " +
                                                    "org varchar(256) not null, " +
                                                    "password varchar(100) not null, " +
                                                    "audit varchar(1024) , " +
                                                    "locales varchar(1024) , " +
                                                    "prefs varchar(1024) , " +
                                                    "intlocs varchar(1024) , " + // added apr 2006: ALTER table CLDR_USERS ADD COLUMN intlocs VARCHAR(1024)
                                                    "lastlogin TIMESTAMP, " + // added may 2006:  alter table CLDR_USERS ADD COLUMN lastlogin TIMESTAMP
                                                    "primary key(id))"); 
            s.execute("INSERT INTO " + CLDR_USERS + "(userlevel,name,org,email,password) " +
                                                    "VALUES(" + ADMIN +"," + 
                                                    "'admin'," + 
                                                    "'SurveyTool'," +
                                                    "'admin@'," +
                                                    "'" + sm.vap +"')");
            logger.info("DB: added user Admin");
            
            s.close();
            conn.commit();
        }
    }
    static final int ADMIN_ID = 1;
    static final int ALL_ID = -1;
    
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
                    importStmt.setString(4,u.real);
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
          queryIdStmt = conn.prepareStatement("SELECT name,org,email,userlevel,intlocs from " + CLDR_USERS +" where id=?",
                                                        ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
          queryEmailStmt = conn.prepareStatement("SELECT id,name,userlevel,org,locales,intlocs,lastlogin from " + CLDR_USERS +" where email=?",
                                                        ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            touchStmt = conn.prepareStatement("UPDATE CLDR_USERS set lastlogin=CURRENT_TIMESTAMP where id=?");
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
      }
    }
    
    /**
     * info = name/email/org
     * immutable info, keep it in a separate list for quick lookup.
     */
    public final static int CHUNKSIZE = 128;
    int arraySize = 0;
    UserRegistry.User infoArray[] = new UserRegistry.User[arraySize];
    
    void zapInfo(int id) {
        synchronized(infoArray) {
            try {
                infoArray[id] = null;
            } catch(IndexOutOfBoundsException ioob) {
                // nothing to do
            }
        }
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
                    u.name = rs.getString(1);
                    u.org = rs.getString(2);
                    u.email = rs.getString(3);
                    u.userlevel = rs.getInt(4);
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
                u.name = rs.getString(2);
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
                logger.info("Login: " + email + " @ " + ip);
                if(!FOR_ADDING.equals(ip)) {
                    touchStmt.setInt(1, u.id);
                    touchStmt.executeUpdate();
                    conn.commit();
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
    SurveyMain sm = null;

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
                zapInfo(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [user level set]";
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
                zapInfo(theirId);
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [locales set]";
                    if(intLocs) { 
                        return null;
                    }
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
                zapInfo(theirId);
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
        u.email = u.email.replace('\'', '_');
        u.org = u.org.replace('\'', '_');
        u.name = u.name.replace('\'', '_');
        u.locales = u.locales.replace('\'', '_');

        synchronized(conn) {
            try {
                logger.info("UR: Attempt newuser by " + ctx.session.user.email + ": of " + u.email + " @ " + ctx.userIP());
                insertStmt.setInt(1, u.userlevel);
                insertStmt.setString(2, u.name);
                insertStmt.setString(3, u.org);
                insertStmt.setString(4, u.email);
                insertStmt.setString(5, u.password);
                insertStmt.setString(6, u.locales);
                if(!insertStmt.execute()) {
                    logger.info("Added.");
                    conn.commit();
                    ctx.println("<p>Added user.<p>");
                    return get(u.password, u.email,FOR_ADDING); // throw away old user
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
        return((u!=null) && userIsStreet(u));
    }
    
    static final boolean userCanModifyLocale(String uLocale, String locale) {
        if(locale.startsWith(uLocale)) {
            int llen = locale.length();
            int ulen = uLocale.length();
            
            if(llen==ulen) {
                return true;  // exact match.   'ro' matches 'ro'
            } else if(!java.lang.Character.isLetter(locale.charAt(ulen))) {
                return true; // next char is NOT a letter. 'ro' matches 'ro_...'
            } else {
                return false; // next char IS a letter.  'ro' DOES NOT MATCH 'root'
            }
        } else {
            return false; // no substring (common case)
        }
    }
    
    static final boolean userCanModifyLocale(String localeArray[], String locale) {
        if(localeArray.length == 0) {
            return true; // all 
        }
        
        for(int i=0;i<localeArray.length;i++) {
            if(userCanModifyLocale(localeArray[i],locale)) {
                return true;
            }
        }
        return false; // no match
    }
    
    static final boolean userCanModifyLocale(User u, String locale) {
        if(u==null) return false; // no user, no dice
        if(userIsTC(u)) return true; // TC can modify all
        if(SurveyMain.phaseClosed) return false;
        if(SurveyMain.phaseSubmit && !userIsStreet(u)) return false;
        if(SurveyMain.phaseVetting && !userIsStreet (u)) return false;
//        if(SurveyMain.phaseVetting && !userIsStreet(u)) return false;
        if(u.locales == null) return true; // empty = ALL
        String localeArray[] = tokenizeLocale(u.locales);
        return userCanModifyLocale(localeArray,locale);
    }

    static final boolean userCanSubmitLocale(User u, String locale) {
        if(u==null) return false; // no user, no dice
        if(userIsTC(u)) return true; // TC can modify all
        if(SurveyMain.phaseClosed) return false;
        if(SurveyMain.phaseVetting && !userIsExpert(u)) return false; // only expert can submit new data.
        return userCanModifyLocale(u,locale);
    }

    static final boolean userCanSubmitAnyLocale(User u) {
        if(u==null) return false; // no user, no dice
        if(userIsTC(u)) return true; // TC can modify all
        if(SurveyMain.phaseClosed) return false;
        if(SurveyMain.phaseVetting && !userIsExpert(u)) return false; // only expert can submit new data.
        return userCanSubmit(u);
    }

    static final boolean userCanVetLocale(User u, String locale) {
        if(userIsTC(u)) return true; // TC can modify all
        if(SurveyMain.phaseClosed) return false;
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
}
