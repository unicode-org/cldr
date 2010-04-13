/* Copyright (C) 2005-2007, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               */
//
//  CookieSession.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/17/2005.
//


package org.unicode.cldr.web;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.*;

/**
 * Instances of this class represent the session-persistent data kept on a per-user basis.
 * Instances are typically held by WebContext.session.
 */
public class CookieSession {
    public String ip;
    public String id;
    public long last;
    public Hashtable stuff = new Hashtable();  // user data
    public Hashtable prefs = new Hashtable(); // user prefs
    public UserRegistry.User user = null;
    
    private Connection conn = null;
    
    public Connection db(SurveyMain sm) {
        if(conn == null) {
            conn = sm.getDBConnection();
        }
        return conn;
    }
    
    /** 
     * Construct a CookieSession from a particular session ID.
     * @param s session ID
     */
    private CookieSession(String s) {
        id = s;
    }
    
    static Hashtable gHash = new Hashtable(); // hash by sess ID
    static Hashtable uHash = new Hashtable(); // hash by user ID
    
    /**
     * return an iterator over all sessions. sorted by age.  For administrative use.
     * @return iterator over all sessions.
     **/
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
    
    /** 
     * Fetch a specific session.  'touch' it (mark it as recently active) if found.
     * @return session or null
     * @param sessionid id to fetch
     */
    public static CookieSession retrieve(String sessionid) {
        CookieSession c = retrieveWithoutTouch(sessionid);
        if(c != null) {
            c.touch();
        }
        return c;
    }

    /** 
     * fetch a session if it exists. Don't touch it as recently active. Useful for 
     * administratively retrieving a session
     * @param sessionid session ID
     * @return session or null
     */
    public static CookieSession retrieveWithoutTouch(String sessionid) {
        synchronized (gHash) {
            CookieSession c = (CookieSession)gHash.get(sessionid);
            return c;
        }
    }
    
    /**
     * Retrieve a user's session. Don't touch it (mark it active) if found.
     * @param email user's email
     * @return session or null
     */
    public static CookieSession retrieveUserWithoutTouch(String email) {
        synchronized (gHash) {
            CookieSession c = (CookieSession)uHash.get(email);
            return c;
        }
    }
    
    /**
     * Retrieve a user's session. Touch it (mark it active) if found.
     * @param email user's email
     * @return session or null
     */
    public static CookieSession retrieveUser(String email) {
        synchronized(gHash) {
            CookieSession c = retrieveUserWithoutTouch(email);
            if(c != null) {
                c.touch();
            }
            return c;
        }
    }
    
    /**
     * Associate this session with a user
     * @param u user
     */
    public void setUser(UserRegistry.User u) {
        user = u;
        synchronized(gHash) {
            uHash.put(user.email, this); // replaces any existing session by this user.
        }
    }
    
    /**
     * Create a bran-new session.  
     * @param isGuest True if the user is a guest.
     */
    public CookieSession(boolean isGuest) {
        id = newId(isGuest);
        touch();
        synchronized(gHash) {
            gHash.put(id,this);
        }
    }
    
    /**
     * mark this session as recently updated and shouldn't expire 
     */
    protected void touch() {
        last = System.currentTimeMillis();
    }
    
    /**
     * Set the user's IP, if known.
     */
    public void setIp(String ip) {
        this.ip=ip;
    }
    
    /**
     * Delete a session.
     */
    public void remove() {
        synchronized(gHash) {
            if(user != null) {
                uHash.remove(user.email);
            }
            gHash.remove(id);
        }
        // clear out any database sessions in use
        SurveyMain.closeDBConnection(conn);
    }
    
    /**
     * How old is this session?
     * @return age of this session, in millis
     */
    protected long age() {
        return (System.currentTimeMillis()-last);
    }
    
    
    static int n = 4000; /** Counter used for ID creation **/
    static int g = 8000; /** Counter used for ID creation **/
    static int h = 90;   /** Counter used for ID creation **/
    public static String j = cheapEncode(System.currentTimeMillis()); /** per instance random hash **/

    // secure stuff
    static SecureRandom myRand = null; /** Secure random number generator **/
    
    /** 
     * Generate a new ID. 
     * @param isGuest true if user is a guest. The guest namespace is separate from the nonguest.
     */
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
    
    //-- convenience functions
    
    /**
     * Get some object out of the session 
     * @param key the key to load
     */
    Object get(String key) { 
        synchronized (stuff) {
            return stuff.get(key);
        }
    }
    
    /**
     * Store an object in the session
     * @param key the key to set
     * @param value object to be set
     */
    void put(String key, Object value) {
        synchronized(stuff) {
            stuff.put(key,value);
        }
    }

    /**
     * Fetch a named boolean from the preferences string
     * @param key parameter to look at
     * @return boolean result, or false as default
     */
    boolean prefGetBool(String key) { 
        return prefGetBool(key,false);
    }

    /**
     * Fetch a named boolean from the preferences string
     * @param key parameter to look at
     * @param defVal default value of parameter
     * @return boolean result, or defVal as default
     */
    boolean prefGetBool(String key, boolean defVal) { 
        Boolean b = (Boolean)prefs.get(key);
        if(b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
    }

    /**
     * Get a string preference
     * @param key the key to load
     * @return named pref, or null as default
     */
    String prefGet(String key) { 
        String b = (String)prefs.get(key);
        if(b == null) {
            return null;
        } else {
            return b;
        }
    }
    
    /** 
     * Store a boolean preference value
     * @param key the pref to put
     * @param value boolean value
     */
    void prefPut(String key, boolean value) {
        prefs.put(key,new Boolean(value));
    }

    /** 
     * Store a string preference value
     * @param key the pref to put
     * @param value string value
     */
    void prefPut(String key, String value) {
        prefs.put(key,value);
    }
    
    /**
     * Fetch a hashtable of per-locale session data. Will create one if it wasn't already there.
     * @return the locale hashtable 
     */
    public Hashtable getLocales() {
        synchronized (stuff) {
            Hashtable l = (Hashtable)get("locales");
            if(l == null) {
                l = new Hashtable();
                put("locales",l);
            }
            return l;
        }
    }
    
    /**
     * Pull an object out of the session according to key and locale
     * @param key key to use
     * @param aLocale locale to fetch by
     * @return the object, or null if not found
     */
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

    /**
     * Store an object into the session according to key and locale
     * @param key key to use
     * @param aLocale locale to store by
     * @param value object value
     */
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
        
    /**
     * remove an object from the session according to key and locale
     * @param key key to use
     * @param aLocale locale to store by
     */
    public final void removeByLocale(String key, String aLocale) {
        synchronized(stuff) {
            Hashtable f = (Hashtable)getLocales().get(aLocale);
            if(f != null) {
                f.remove(key);
            }
        }
    }
    
    /**
     * utility function for doing an encoding of a long.  Creates a base26 (a-z) representation of the number, plus a leading '-' if negative.
     * TODO: shrink? base64?
     * @param l some number
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
    
    
    /**
     * utility function for doing a base64 of some bytes.  URL safe, converts '=/+' to ',._' respectively.
     * @param b some data in bytes
     * @return string
     */
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
    
    
    //- Session Reaping.  Deletion of old user session
    static final int MILLIS_IN_MIN = 1000*60; /**  1 minute = 60,000 milliseconds **/
// testing:
//    static final int GUEST_TO =  1 * MILLIS_IN_MIN; // Expire Guest sessions after three hours
//    static final int USER_TO =  3 * MILLIS_IN_MIN; // soon.
//    static final int REAP_TO = 8000; //often.
// production:
    public static final int GUEST_TO =  10 * MILLIS_IN_MIN; /** Expire Guest sessions after 10 min **/
    public static final int GUEST_TO_10 =  2 * MILLIS_IN_MIN; /** Expire Guest sessions after 2 min **/
    public static final int GUEST_TO_40 =  0; /** Expire Guest sessions immediately **/
    
    public static final int USER_TO =  1 * 60 * MILLIS_IN_MIN; /** Expire non-guest sessions after 1 hours **/
    public static final int REAP_TO = 4 * MILLIS_IN_MIN; /** Only reap once every 4 mintutes **/
    public static final int REAP_TO_10 = 30*1000; /** Only reap once every 4 mintutes **/
    public static final int REAP_TO_40 = 0; /** Only reap once every 4 mintutes **/

    static long lastReap = 0; /** last time reaped.  Starts at 0, so reap immediately **/
    
    public static int nGuests = 0; /** # of guests **/
    public static int nUsers = 0; /** # of users **/
    
    /**
     * Perform a reap.  To be called from SurveyMain etc.
     */
    public static void reap() {        
        synchronized(gHash) {
            int allCount = gHash.size(); // count of ALL users
            // reap..
            int reap_to = REAP_TO;
            if(allCount > 40) {
                reap_to = REAP_TO_40;
            } else if(allCount > 10) {
                reap_to = REAP_TO_10;
            }
            int guest_to = GUEST_TO;
            if(allCount > 40) {
                guest_to = GUEST_TO_40;
            } else if(allCount > 10) {
                guest_to = GUEST_TO_10;
            }
            
            long elapsed = (System.currentTimeMillis()-lastReap);
            
//            System.err.println("reap: elapsed " + elapsed + ", nGuests " + nGuests +", reap_to:"+reap_to+", guest_to:"+guest_to + ", gHash count: " + gHash.size());
            
            if(elapsed < reap_to) {
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
                    if(cs.age() > guest_to) {
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

    public static void shutdownDB() {
        synchronized(gHash) {
            CookieSession sessions[] = (CookieSession[])gHash.values().toArray(new CookieSession[0]);
            for(CookieSession cs : sessions) {
                try {
                    cs.remove();
                } catch(Throwable t) {
                    //
                }
            }
        }
    }
}
