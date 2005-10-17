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

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;


public class UserRegistry {
    // user levels
    public static final int ADMIN   = 0;
    public static final int TC      = 1;
    public static final int VETTER  = 5;
    public static final int STREET  = 10;
    public static final int LOCKED  = 999;
    
    
    
    public class User {
        public int    level;    // user level
        public String id;       // password
        public String email;    // 
        public String sponsor;  // organization
        public String real;     // full name
        public Date last_connect;
    }
    
    private static final String CLDR_USERS = "cldr_users";
    
    /* +INT id
INT userlevel - one of the following
VARCHAR(40) full_name
VARCHAR(40) email_address
VARCHAR(20) org
This is the organization (or company) sponsoring the account
For level 1 tc: it is which organization's vetters they manage
For level 5 vetter it is whom they are managed by
For level 10 street: it is only advisory as to who authorized the account
VARCHAR(20) password
DATE last_connect
preferences..?

    cldrxpaths

this will probably be read into memory as a cache and then written back out.
singleton in memory (with its own db connection?) - on 'add' writes and commits to DB
+INT id
VARCHAR(200) xpath

- cldrdata
id xpath
string proposaltype
varchar data
*/
    public static void setupDB(SurveyMain sm, Connection conn) throws SQLException
    {
        sm.appendLog("UserRegistry DB: initializing...");
        Statement s = conn.createStatement();
        s.execute("create table " + CLDR_USERS + "(id int not null auto_increment , " +
                                                "userlevel int not null, " +
                                                "full_name varchar(40) not null, " +
                                                "email_address varchar(40) not null, " +
                                                "org varchar(20) not null, " +
                                                "password varchar(20)) not null, " +
                                                "primary key(id)");
        sm.appendLog("DB: Created table " + CLDR_USERS);
        s.close();
        sm.appendLog("UserRegistry DB: done");
    }
    
    public UserRegistry.User get(String uid, String email) {
        return null;
    }

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

    public UserRegistry(SurveyMain ourSM, Connection ourConn) {
        sm = ourSM;
        conn = ourConn;
    }
}
