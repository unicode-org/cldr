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


public class UserRegistry {
    private static java.util.logging.Logger logger;
    // user levels
    public static final int ADMIN   = 0;
    public static final int TC      = 1;
    public static final int VETTER  = 5;
    public static final int STREET  = 10;
    public static final int LOCKED  = 999;
    
    public static final int ALL_LEVELS[] = { // for UI presentation
        ADMIN, TC, VETTER, STREET, LOCKED };
    

    public static String levelToStr(WebContext ctx, int level) {
        String thestr = null;
        if(level <= ADMIN) { 
            thestr = "ADMIN";
        } else if(level <= TC) {
            thestr = "TC";
        } else if (level <= VETTER) {
            thestr = "VETTER";
        } else if (level <= STREET) {
            thestr = "STREET";
        } else if (level == LOCKED) {
            thestr = "LOCKED";
        } else {
            thestr = "??";
        }
        return level + ": (" + thestr + ")";
    }
    
    PreparedStatement insertStmt = null;
    PreparedStatement importStmt = null;
    PreparedStatement queryStmt = null;
    PreparedStatement queryIdStmt = null;
    PreparedStatement queryEmailStmt = null;
    
    public class User {
        public int    id;  // id number
        public int    userlevel;    // user level
        public String password;       // password
        public String email;    // 
        public String org;  // organization
        public String name;     // full name
        public Date last_connect;
        public String locales;

        public void printPasswordLink(WebContext ctx) {
            UserRegistry.printPasswordLink(ctx, email, password);
        }
        
    }
        
    public static void printPasswordLink(WebContext ctx, String email, String password) {
        ctx.println("<a href='" + ctx.base() + "?email=" + email + "&uid=" + password + "'>Login for " + 
            email + "</a>");
    }
    
    private static final String CLDR_USERS = "cldr_users";
    
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
        logger.info("UserRegistry DB: created");
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
            logger.info("UserRegistry DB: initializing...");
            Statement s = conn.createStatement();
            s.execute("create table " + CLDR_USERS + "(id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                                                    "userlevel int not null, " +
                                                    "name varchar(40) not null, " +
                                                    "email varchar(40) not null UNIQUE, " +
                                                    "org varchar(20) not null, " +
                                                    "password varchar(20) not null, " +
                                                    "audit varchar(20) , " +
                                                    "locales varchar(20) , " +
                                                    "prefs varchar(20) , " +
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

          queryStmt = conn.prepareStatement("SELECT id,name,userlevel,org,locales from " + CLDR_USERS +" where email=? AND password=?");
          queryEmailStmt = conn.prepareStatement("SELECT id,name,userlevel,org,locales from " + CLDR_USERS +" where email=?");
        }
      }finally{
        if(queryStmt == null) {
            logger.severe("queryStmt failed to initialize");
        }
        if(insertStmt == null) {
            logger.severe("insertStmt failed to initialize");
        }
      }
    }
    
    public  UserRegistry.User get(String pass, String email) {
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
                    logger.info("Unknown user or bad login: " + email);
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
                
                // good so far..
                
                if(rs.next()) {
                    // dup returned!
                    logger.severe("Duplicate user for " + email + " - ids " + u.id + " and " + rs.getInt(1));
                    return null;
                }
                logger.info("Login: " + email);
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
        return get(null,email);
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
    /*
    public UserRegistry.User add(WebContext ctx, String email, String sponsor, String name, String requester) {
        return null;
    }
    public boolean read() {
        return false; // always broken
    }
    */
    
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
                    rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales FROM " + CLDR_USERS + ORDER);
                } else {
                    rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales FROM " + CLDR_USERS + " WHERE org='" + organization + "'" + ORDER);
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
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [completed OK]";
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
        if(ctx.session.user.userlevel > TC) { // Note- we dont' check that a TC isn't modifying an Admin's locale. 
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
                String theSql = "UPDATE " + CLDR_USERS + " SET locales=? WHERE id=" + theirId + " AND email='" + theirEmail + "' "  + orgConstraint;
                PreparedStatement ps = conn.prepareStatement(theSql);
      //           msg = msg + " (<br /><pre> " + theSql + " </pre><br />) ";
                logger.info("Attempt user locales update by " + ctx.session.user.email + ": " + theSql + " - " + newLocales);
                ps.setString(1, newLocales);
                int n = ps.executeUpdate();
                conn.commit();
                if(n == 0) {
                    msg = msg + " [Error: no users were updated!] ";
                    logger.severe("Error: 0 records updated.");
                } else if(n != 1) {
                    msg = msg + " [Error in updating users!] ";
                    logger.severe("Error: " + n + " records updated!");
                } else {
                    msg = msg + " [completed OK]";
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
        return  CookieSession.cheapEncode((System.currentTimeMillis()*100) + SurveyMain.pages) + "x" + 
            CookieSession.cheapEncode(email.hashCode() * SurveyMain.vap.hashCode());
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
                logger.info("UR: Attempt newuser by " + ctx.session.user.email + ": of " + u.email);
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
                    return get(u.password, u.email); // throw away old user
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
}
