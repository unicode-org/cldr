/* Copyright (C) 2004-2005, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               */
//
//  CookieJar.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/17/2005.
//  Copyright 2005 IBM. All rights reserved.
//


package org.unicode.cldr.web;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CookieSession {
    public String ip;
    public String id;
    public long last;
    public Hashtable stuff = new Hashtable();  // user data
    public Hashtable prefs = new Hashtable(); // user prefs
    public UserRegistry.User user = null;
    
    private CookieSession(String s) {
        id = s;
    }
    
    static Hashtable gHash = new Hashtable(); // hash by sess ID
    static Hashtable uHash = new Hashtable(); // hash by user ID
    
    /* all sessions. sorted by age. */
    public static Iterator getAll() {
        synchronized(gHash) {
            TreeSet sessSet = new TreeSet(new Comparator() {
                  public int compare(Object a, Object b) {
                    CookieSession aa = (CookieSession)a;
                    CookieSession bb = (CookieSession)b;
                    if(aa==bb) return 0;
                    if(aa.last>bb.last) return -1;
                    if(aa.last<bb.last) return 1;
                    return 0; // same age
                   }
                });
    //      sessSet.addAll(uHash.values()); // all users (reg'd)
            sessSet.addAll(gHash.values()); // ALL sessions
            return sessSet.iterator();
            //return uHash.values().iterator();
        }
    }
    
    public static CookieSession retrieve(String sessionid) {
        CookieSession c = retrieveWithoutTouch(sessionid);
        if(c != null) {
            c.touch();
        }
        return c;
    }

    public static CookieSession retrieveWithoutTouch(String sessionid) {
        synchronized (gHash) {
            CookieSession c = (CookieSession)gHash.get(sessionid);
            return c;
        }
    }
    
    public static CookieSession retrieveUserWithoutTouch(String email) {
        synchronized (gHash) {
            CookieSession c = (CookieSession)uHash.get(email);
            return c;
        }
    }
    
    public static CookieSession retrieveUser(String email) {
        synchronized(gHash) {
            CookieSession c = retrieveUserWithoutTouch(email);
            if(c != null) {
                c.touch();
            }
            return c;
        }
    }
    
    public void setUser(UserRegistry.User u) {
        user = u;
        synchronized(gHash) {
            uHash.put(user.email, this); // replaces any existing session by this user.
        }
    }
    
    public CookieSession(boolean isGuest) {
        id = newId(isGuest);
        touch();
        synchronized(gHash) {
            gHash.put(id,this);
        }
    }
    
    protected void touch() {
        last = System.currentTimeMillis();
    }
    
    public void setIp(String ip) {
        this.ip=ip;
    }
    
    public void remove() {
        synchronized(gHash) {
            if(user != null) {
                uHash.remove(user.email);
            }
            gHash.remove(id);
        }
    }
    
    protected long age() {
        return (System.currentTimeMillis()-last);
    }
    
    static int n = 4000;
    static int g = 8000;
    static int h = 90;
    public static String j = cheapEncode(System.currentTimeMillis());

    // secure stuff
    static SecureRandom myRand = null;
    
    public static synchronized String newId(boolean isGuest) {
        try {
            if(myRand == null) {
                myRand = SecureRandom.getInstance("SHA1PRNG");
            }

            MessageDigest aDigest = MessageDigest.getInstance("SHA-1");
            byte[] outBytes = aDigest.digest( new Integer(myRand.nextInt()).toString().getBytes() );
            return cheapEncode(outBytes);
        } catch(NoSuchAlgorithmException nsa) {
            System.err.println(nsa.toString() + " - falling back..");
            if(isGuest) {
                // no reason, just a different set of hashes
                return cheapEncode(h+=2)+"w"+cheapEncode(j.hashCode()+g++);
            } else {
                return cheapEncode((n+=(j.hashCode()%444))+n) +"y" + j;
            }        
    }
    }
    // convenience functions
    Object get(String key) { 
        synchronized (stuff) {
            return stuff.get(key);
        }
    }
    
    void put(String key, Object value) {
        synchronized(stuff) {
            stuff.put(key,value);
        }
    }

    boolean prefGetBool(String key) { 
        return prefGetBool(key,false);
    }
    boolean prefGetBool(String key, boolean defVal) { 
        Boolean b = (Boolean)prefs.get(key);
        if(b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
    }

    String prefGet(String key) { 
        String b = (String)prefs.get(key);
        if(b == null) {
            return null;
        } else {
            return b;
        }
    }
    
    void prefPut(String key, boolean value) {
        prefs.put(key,new Boolean(value));
    }

    void prefPut(String key, String value) {
        prefs.put(key,value);
    }
    
    public Hashtable getLocales() {
        Hashtable l = (Hashtable)get("locales");
        if(l == null) {
            l = new Hashtable();
            put("locales",l);
        }
        return l;
    }
    
    public final Object getByLocale(String key, String aLocale) {
        synchronized(stuff) {
            Hashtable f = (Hashtable)getLocales().get(aLocale);
            if(f != null) {
                return f.get(key);
            } else {
                return null;
            }
        }
    }

    public void putByLocale(String key, String aLocale, Object value) {
        synchronized(stuff) {
            Hashtable f = (Hashtable)getLocales().get(aLocale);
            if(f == null) {
                f = new Hashtable();
                getLocales().put(aLocale, f);
            }
            f.put(key,value);
        }
    }
        
    public final void removeByLocale(String key, String aLocale) {
        synchronized(stuff) {
            Hashtable f = (Hashtable)getLocales().get(aLocale);
            if(f != null) {
                f.remove(key);
            }
        }
    }
    
    /**
     * utility function for doing a hash
     * @param l number
     * @return string
     */
    public static String cheapEncode(long l) {
        String out = "";
        if(l < 0) {
            out = out+"-";
            l = 0 - l;
        } else if (l == 0) {
            return "0";
        }
        while(l > 0) {
            char c = (char)(l%(26));
            char o;
            c += 'a';
            o = c;
            out = out + o;
            l /= 26;
        }
        return out;
    }
    
    public static String cheapEncode(byte b[]) {
        StringBuffer sb = new StringBuffer(new sun.misc.BASE64Encoder().encode(b));
        for(int i=0;i<sb.length();i++) {
            char c = sb.charAt(i);
            if(c == '=') {
                sb.setCharAt(i,',');
            } else if(c == '/') {
                sb.setCharAt(i,'.');
            } else if(c == '+') {
                sb.setCharAt(i,'_');
            }
        }
        return sb.toString();
    }
    
    
    // Reaping. 
    // For now, we just reap *guest* sessions and not regular users. 
    static final int MILLIS_IN_MIN = 1000*60; // 1 minute = 60,000 milliseconds
// testing:
//    static final int GUEST_TO =  1 * MILLIS_IN_MIN; // Expire Guest sessions after three hours
//    static final int USER_TO =  3 * MILLIS_IN_MIN; // soon.
//    static final int REAP_TO = 8000; //often.
// production:
    public static final int GUEST_TO =  10 * MILLIS_IN_MIN; // Expire Guest sessions after 15 min
    public static final int USER_TO =  1 * 60 * MILLIS_IN_MIN; // Expire non-guest sessions after a few hours
    public static final int REAP_TO = 4 * MILLIS_IN_MIN; // Only once every 4 mintutes

    static long lastReap = 0; // reap once, to get a count..
    
    public static int nGuests = 0;
    public static int nUsers = 0;
    
    public static void reap() {        
        synchronized(gHash) {
            // reap..
            if((System.currentTimeMillis()-lastReap) < REAP_TO) {
                return;
            }
            int guests=0;
            int users=0;
            lastReap=System.currentTimeMillis();
           // System.out.println("reaping..");
            // step 0: reap all guests older than time
            for(Iterator i = gHash.values().iterator();i.hasNext();) {
                CookieSession cs = (CookieSession)i.next();
                
                if(cs.user == null) {
                    if(cs.age() > GUEST_TO) {
//                        System.out.println("Reaped guest session: " + cs.id + " after  " + SurveyMain.timeDiff(cs.last) +" inactivity.");
                        cs.remove();
                        // concurrent modify . . . (i.e. rescan.)
                        i = gHash.values().iterator();
                    } else {
                        guests++;
                    }
                } else {
                    if(cs.age() > USER_TO) {
//                        System.out.println("Reaped users session: " + cs.id + " (" + cs.user.email + ") after  " + SurveyMain.timeDiff(cs.last) +" inactivity.");
                        cs.remove();
                        // concurrent modify . . . (i.e. rescan.)
                        i = gHash.values().iterator();
                    } else {
                        users++;
                    }
                }
            }
            nGuests=guests;
            nUsers=users;
        }
    }
}
