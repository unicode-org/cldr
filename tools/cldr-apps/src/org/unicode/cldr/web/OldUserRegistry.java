//
//  UserRegistry.java  (for the CLDR 1.3 registry)
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;

/**
 * The 'old' format user registry, consisting of a flat text file (cldrvet.txt).
 * Read-only, and only used for bootstrapping from an empty DB.
 * @deprecated
 */
public class OldUserRegistry {
    public static final char SEPARATOR = '\t';
    public static final String REGISTRY = "cldrvet.txt";

    public class User {
        public String id;       // 0
        public String email;    // 1
        public String sponsor;  // 2
        public String real;     // 3
    }
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
    public Hashtable getEmails() {
        return emails;
    }
    Hashtable emails = new Hashtable();
    /**
     * returns false if there was a failure reading. 
     */
    String dir;
    String filename;
    public OldUserRegistry(String aDir) {
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
    
    synchronized User add(WebContext ctx, String email, String sponsor, String real, String requester) {
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
        write(ctx, u, requester);
        
        return u;
    }

    /**
     * internal put
     */
    private synchronized void put(User u) {
        users.put(u.id,u);
        emails.put(u.email,u);
        userCount++;
    }
    
    private synchronized void write(WebContext ctx, User u, String requester) {
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
          pw.println("# IP: " + ctx.userIP());
          pw.println("# Requester: " + requester);
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
