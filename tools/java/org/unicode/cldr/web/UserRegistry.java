//
//  UserRegistry.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

public class UserRegistry {
    public static final char SEPARATOR = '\t';
    public static final String REGISTRY = "cldrvet.txt";

    public class User {
        public String id;       // 0
        public String email;    // 1
        public String sponsor;  // 2
        public String real;     // 3
    };
    public User getEmptyUser() {
        User u = new User();
        u.id = null;
        u.real = "UNKNOWN";
        u.email = "UNKNOWN - UNVERIFIED";
        u.sponsor = "NONE"; 
        
       return u;   
    }
    int lines = 0;
    int userCount = 0;
    
    Hashtable users = new Hashtable();
    Hashtable emails = new Hashtable();
    /**
     * returns false if there was a failure reading. 
     */
    String dir;
    String filename;
    public UserRegistry(String aDir) {
        dir = aDir;
        filename = dir + "/" + REGISTRY;
    }
    boolean read() {
        try {
            BufferedReader in
               = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine())!=null) {
                lines++;
                if((line.length()<=0) ||
                  (line.charAt(0)=='#')) {
                    continue;
                }
                String[] result = line.split("\t");
                User u    = new User();
                u.id      = result[0];
                u.email   = result[1];
                u.sponsor = result[2];
                u.real    = result[3];
                put(u);
            }
        } catch (Throwable t) {
            System.err.println(t.toString());
            t.printStackTrace();
            System.err.println("Can't read registry file: " + filename);
            System.err.println("At least create an empty file there. Exitting.");
            return false;
        }
    //    System.err.println(filename + ": " + lines + " lines, " + userCount + " users");
        return true;
    }
    
    /**
     * for validation. returns null if BOTH don't match. 
     */
    User get(String id, String email) {
        User u = (User)users.get(id);
        if(u == null)  {
            return null;
        }
        if(!u.email.equals(email)) {
            return null;
        }
        return u;
    }
    
    /**
     * for resending password, or creating new
     */
    User get(String email)
    {
        return (User)emails.get(email);
    }
    
    User add(String email, String sponsor, String real) {
        if(get(email) != null) {
            return null; // already exists.
        }
        User u = new User();
        u.id = CookieSession.cheapEncode((System.currentTimeMillis()*100) + userCount) + "x" + 
            CookieSession.cheapEncode(email.hashCode() * SurveyMain.vap.hashCode());
        u.email = email;
        u.sponsor = sponsor;
        u.real = real;
        put(u); // add to hashtable
        write(u);
        
        return u;
    }

    /**
     * internal put
     */
    private void put(User u) {
        users.put(u.id,u);
        emails.put(u.email,u);
        userCount++;
    }
    
    private synchronized void write(User u) {
        try {
          OutputStream file = new FileOutputStream(filename, true); // Append
          PrintWriter pw = new PrintWriter(file);
          if(lines == 0) { // write the prologue
            pw.println("## cldr survey registry file.");
            pw.println("# Items are separated by tabs. ");
            pw.println("# Comments describe items immediately following.");
            pw.println("# ID email   sponsor real");
            lines++;
          }
          pw.println("#---------------");
          pw.println("# Date: " + new Date().toString());
          pw.println("# IP: " + WebContext.userIP());
          pw.println(u.id + SEPARATOR + 
                     u.email + SEPARATOR +
                     u.sponsor + SEPARATOR +
                     u.real);
         pw.close();
         file.close();
        }
        catch(IOException exception){
          System.err.println(exception);
          // TODO: log this ... 
        }
    }
}
