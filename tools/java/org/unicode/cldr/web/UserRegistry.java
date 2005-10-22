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


public class UserRegistry {
    private static java.util.logging.Logger logger;
    // user levels
    public static final int ADMIN   = 0;
    public static final int TC      = 1;
    public static final int VETTER  = 5;
    public static final int STREET  = 10;
    public static final int LOCKED  = 999;
    
    java.sql.PreparedStatement insertStmt = null;
    java.sql.PreparedStatement queryStmt = null;
    java.sql.PreparedStatement queryIdStmt = null;
    
    public class User {
        public int    id;  // id number
        public int    userlevel;    // user level
        public String password;       // password
        public String email;    // 
        public String org;  // organization
        public String name;     // full name
        public Date last_connect;
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
        }
    }
    
    private void myinit() throws SQLException {
     try {
        synchronized(conn) {
//        insertStmt = 
          queryStmt = conn.prepareStatement("SELECT id,name,userlevel,org from " + CLDR_USERS +" where email=? AND password=?");
//        queryIdStmt
        }
      }finally{
        if(queryStmt == null) {
            logger.severe("queryStmt failed to initialize");
        }
      }
    }
    
    public  UserRegistry.User get(String pass, String email) {
        ResultSet rs = null;
        synchronized(conn) {
            try{ 
                logger.info("Looking up " + email + " : " + pass);
                queryStmt.setString(1,email);
                queryStmt.setString(2,pass);
                // First, try to query it back from the DB.
                rs = queryStmt.executeQuery();                
                if(!rs.next()) {
                    logger.info(".. no match.");
                    return null;
                }
                User u = new UserRegistry.User();
                
                // from params:
                u.password = pass;
                u.email = email;
                // from db:   (id,name,userlevel,org)
                u.id = rs.getInt(1);
                u.name = rs.getString(2);
                u.userlevel = rs.getInt(3);
                u.org = rs.getString(4);
                
                // good so far..
                
                if(rs.next()) {
                    // dup returned!
                    logger.severe("Duplicate user for " + email + " - ids " + u.id + " and " + rs.getInt(1));
                    return null;
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
        return null;
    }
    public UserRegistry.User getEmptyUser() {
        return null;
    }
    public UserRegistry.User add(WebContext ctx, String email, String sponsor, String name, String requester) {
        return null;
    }
    public boolean read() {
        return false; // always broken
    }
    
    Connection conn = null;
    SurveyMain sm = null;

    private UserRegistry(java.util.logging.Logger xlogger, Connection ourConn) {
        logger = xlogger;
        conn = ourConn;
    }
    
    public java.sql.ResultSet list(String organization) throws SQLException {
        ResultSet rs = null;
        Statement s = null;
        final String ORDER = " ORDER BY org,userlevel,name ";
        synchronized(conn) {
//            try {
                s = conn.createStatement();
                if(organization == null) {
                    rs = s.executeQuery("SELECT userlevel,name,email,org FROM " + CLDR_USERS + ORDER);
                } else {
                    rs = s.executeQuery("SELECT userlevel,name,email,org FROM " + CLDR_USERS + " WHERE org='" + organization + "'" + ORDER);
                }
//            } finally  {
//                s.close();
//            }
        }
        
        return rs;
    }
}
