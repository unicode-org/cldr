/* Copyright (C) 2005-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               */
//
//  CookieSession.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/17/2005.
//

package org.unicode.cldr.web;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;

/**
 * Instances of this class represent the session-persistent data kept on a
 * per-user basis. Instances are typically held by WebContext.session.
 */
public class CookieSession {
    static final boolean DEBUG_INOUT = false;
    public String id;
    public String ip;
    public Hashtable<String, Object> stuff = new Hashtable<String, Object>(); // user data
    public Hashtable<String, Comparable> prefs = new Hashtable<String, Comparable>(); // user prefs
    public UserRegistry.User user = null;
    /**
     * @deprecated need to refactor anything that uses this.
     */
    public static SurveyMain sm = null;

    private Connection conn = null;
    /**
     * When did the user last take an explicit action?
     */
    private long lastAction = System.currentTimeMillis();

    public long getLastAction() {
        return lastAction;
    }

    /**
     * How long, in ms, before we kick?
     * @return
     */
    public long timeTillKick() {
        final long now = System.currentTimeMillis();
        final boolean guest = (user == null);
        long myTimeout; // timeout in seconds.

        if (user != null && UserRegistry.userIsTC(user)) {
            myTimeout = 60 * 20; // 20 minutes
        } else if (guest) {
            if (!tooManyGuests()) {
                myTimeout = 60 * 10; // 10 minutes
            } else {
                myTimeout = Params.CLDR_GUEST_INACTIVITY.value();
            }
        } else {
            if (!tooManyUsers()) {
                myTimeout = 60 * 20; // 20 min
            } else {
                myTimeout = Params.CLDR_USER_INACTIVITY.value();
            }
        }

        final long remain = (1000 * myTimeout) - (now - lastAction);

        if (remain < 0) {
            return 0;
        } else {
            return remain;
        }
    }

    /**
     * When did the user last touch this session?
     */
    public long last;

    public String toString() {
        return "{CookieSession#" + id + ", user=" + user + ", timeTillKick=" + SurveyMain.durationDiff(timeTillKick()) + ", age=" + age() + ", userActionAge="
            + userActionAge() + "}";
    }

    static Hashtable<String, CookieSession> gHash = new Hashtable<String, CookieSession>(); // hash by sess ID
    static Hashtable<String, CookieSession> uHash = new Hashtable<String, CookieSession>(); // hash by user ID

    public static Set<CookieSession> getAllSet() {
        synchronized (gHash) {
            TreeSet<CookieSession> sessSet = new TreeSet<CookieSession>(new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    CookieSession aa = (CookieSession) a;
                    CookieSession bb = (CookieSession) b;
                    if (aa == bb)
                        return 0;
                    if (aa.last > bb.last)
                        return -1;
                    if (aa.last < bb.last)
                        return 1;
                    return 0; // same age
                }
            });
            // sessSet.addAll(uHash.values()); // all users (reg'd)
            sessSet.addAll(gHash.values()); // ALL sessions
            return sessSet;
            // return uHash.values().iterator();
        }
    }

    /**
     * Fetch a specific session. 'touch' it (mark it as recently active) if
     * found.
     *
     * @return session or null
     * @param sessionid
     *            id to fetch
     */
    public static CookieSession retrieve(String sessionid) {
        CookieSession c = retrieveWithoutTouch(sessionid);
        if (c != null) {
            c.touch();
        }
        return c;
    }

    /**
     * fetch a session if it exists. Don't touch it as recently active. Useful
     * for administratively retrieving a session
     *
     * @param sessionid
     *            session ID
     * @return session or null
     */
    public static CookieSession retrieveWithoutTouch(String sessionid) {
        checkForExpiredSessions();
        synchronized (gHash) {
            CookieSession c = gHash.get(sessionid);
            return c;
        }
    }

    /**
     * Retrieve a user's session. Don't touch it (mark it active) if found.
     *
     * @param email
     *            user's email
     * @return session or null
     */
    public static CookieSession retrieveUserWithoutTouch(String email) {
        synchronized (gHash) {
            CookieSession c = uHash.get(email);
            return c;
        }
    }

    /**
     * Retrieve a user's session. Touch it (mark it active) if found.
     *
     * @param email
     *            user's email
     * @return session or null
     */
    public static CookieSession retrieveUser(String email) {
        synchronized (gHash) {
            CookieSession c = retrieveUserWithoutTouch(email);
            if (c != null) {
                c.touch();
            }
            return c;
        }
    }

    /**
     * Associate this session with a user
     *
     * @param u
     *            user
     */
    public void setUser(UserRegistry.User u) {
        user = u;
        settings = null;
        synchronized (gHash) {
            uHash.put(user.email, this); // replaces any existing session by
            // this user.
        }
    }

    /**
     * Create a new session.
     *
     * @param isGuest
     *            True if the user is a guest.
     */

    private CookieSession(boolean isGuest, String ip, String fromId) {
        this.ip = ip;
        if (fromId == null) {
            id = newId(isGuest);
        } else {
            id = fromId;
        }
        if (DEBUG_INOUT) System.out.println("S: new " + id + " - " + user);
        synchronized (gHash) {
            if (gHash.containsKey(id)) {
                System.err.println("CookieSession.CookieSession() - dup id " + id);
            }
            gHash.put(id, this);
            touch();
        }
    }

    public static CookieSession newSession(boolean isGuest, String ip, String fromId) {
        CookieSession rv = null;
        synchronized (gHash) {
            rv = gHash.get(fromId);
            if (rv != null) {
                System.err.println("Trying to create extant session " + rv);
                if (!rv.ip.equals(ip)) {
                    if (SurveyMain.isUnofficial()) System.out.println("IP changed from " + rv.ip + " to " + ip + " - " + rv);
                    rv.ip = ip;
                    rv.touch();
                }
            } else {
                rv = new CookieSession(isGuest, ip, fromId);
            }
        }
        return rv;
    }

    /**
     * mark this session as recently updated and shouldn't expire
     */
    protected void touch() {
        last = System.currentTimeMillis();
        if (DEBUG_INOUT) System.out.println("S: touch " + id + " - " + user);
    }

    /**
     * Note a direct user action.
     */
    public void userDidAction() {
        lastAction = System.currentTimeMillis();
    }

    /**
     * Delete a session.
     */
    public void remove() {
        synchronized (gHash) {
            if (user != null) {
                uHash.remove(user.email);
            }
            gHash.remove(id);
        }
        // clear out any database sessions in use
        DBUtils.closeDBConnection(conn);
        if (DEBUG_INOUT) System.out.println("S: Removing session: " + id + " - " + user);
    }

    /**
     * How old is this session? (last time we heard from their browser, at all)
     *
     * @return age of this session, in millis
     */
    protected long age() {
        return (System.currentTimeMillis() - last);
    }

    /**
     * How old is this session's last active action? (last voted, viewed a page, etc)
     *
     * @return age of this session, in millis
     */
    protected long userActionAge() {
        return (System.currentTimeMillis() - lastAction);
    }

    // secure stuff
    static SecureRandom myRand = null;

    /** Secure random number generator **/

    /**
     * Generate a new ID.
     *
     * @param isGuest
     *            true if user is a guest. The guest namespace is separate from
     *            the nonguest.
     */
    public static synchronized String newId(boolean isGuest) {
        try {
            if (myRand == null) {
                myRand = SecureRandom.getInstance("SHA1PRNG");
            }

            MessageDigest aDigest = MessageDigest.getInstance("SHA-1");
            byte[] outBytes = aDigest.digest(new Integer(myRand.nextInt()).toString().getBytes());
            return cheapEncode(outBytes);
        } catch (NoSuchAlgorithmException nsa) {
            SurveyMain.busted("MessageDigest error", nsa);
            return null;
        }
    }

    // -- convenience functions

    /**
     * Get some object out of the session
     *
     * @param key
     *            the key to load
     */
    Object get(String key) {
        synchronized (stuff) {
            return stuff.get(key);
        }
    }

    /**
     * Store an object in the session
     *
     * @param key
     *            the key to set
     * @param value
     *            object to be set
     */
    public void put(String key, Object value) {
        synchronized (stuff) {
            stuff.put(key, value);
        }
    }

    /**
     * Fetch a named boolean from the preferences string
     *
     * @param key
     *            parameter to look at
     * @return boolean result, or false as default
     */
    boolean prefGetBool(String key) {
        return prefGetBool(key, false);
    }

    /**
     * Fetch a named boolean from the preferences string
     *
     * @param key
     *            parameter to look at
     * @param defVal
     *            default value of parameter
     * @return boolean result, or defVal as default
     */
    boolean prefGetBool(String key, boolean defVal) {
        Boolean b = (Boolean) prefs.get(key);
        if (b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
    }

    /**
     * Get a string preference
     *
     * @param key
     *            the key to load
     * @return named pref, or null as default
     */
    String prefGet(String key) {
        String b = (String) prefs.get(key);
        if (b == null) {
            return null;
        } else {
            return b;
        }
    }

    /**
     * Store a boolean preference value
     *
     * @param key
     *            the pref to put
     * @param value
     *            boolean value
     */
    void prefPut(String key, boolean value) {
        prefs.put(key, new Boolean(value));
    }

    /**
     * Store a string preference value
     *
     * @param key
     *            the pref to put
     * @param value
     *            string value
     */
    void prefPut(String key, String value) {
        prefs.put(key, value);
    }

    /**
     * Fetch a hashtable of per-locale session data. Will create one if it
     * wasn't already there.
     *
     * @return the locale hashtable
     */
    public Hashtable<String, Hashtable<String, Object>> getLocales() {
        synchronized (stuff) {
            Hashtable<String, Hashtable<String, Object>> l = (Hashtable<String, Hashtable<String, Object>>) get("locales");
            if (l == null) {
                l = new Hashtable<String, Hashtable<String, Object>>();
                put("locales", l);
            }
            return l;
        }
    }

    /**
     * Pull an object out of the session according to key and locale
     *
     * @param key
     *            key to use
     * @param aLocale
     *            locale to fetch by
     * @return the object, or null if not found
     */
    public final Object getByLocale(String key, String aLocale) {
        synchronized (stuff) {
            Hashtable<String, Object> f = getLocales().get(aLocale);
            if (f != null) {
                return f.get(key);
            } else {
                return null;
            }
        }
    }

    /**
     * Store an object into the session according to key and locale
     *
     * @param key
     *            key to use
     * @param aLocale
     *            locale to store by
     * @param value
     *            object value
     */
    public void putByLocale(String key, String aLocale, Object value) {
        synchronized (stuff) {
            Hashtable<String, Object> f = getLocales().get(aLocale);
            if (f == null) {
                f = new Hashtable<String, Object>();
                getLocales().put(aLocale, f);
            }
            f.put(key, value);
        }
    }

    /**
     * remove an object from the session according to key and locale
     *
     * @param key
     *            key to use
     * @param aLocale
     *            locale to store by
     */
    public final void removeByLocale(String key, String aLocale) {
        synchronized (stuff) {
            Hashtable<String, Object> f = getLocales().get(aLocale);
            if (f != null) {
                f.remove(key);
            }
        }
    }

    /**
     * utility function for doing an encoding of a long. Creates a base26 (a-z)
     * representation of the number, plus a leading '-' if negative. TODO:
     * shrink? base64?
     *
     * @param l
     *            some number
     * @return string
     */
    public static String cheapEncode(long l) {
        StringBuilder out = new StringBuilder(10);
        if (l < 0) {
            out.append("-");
            l = 0 - l;
        } else if (l == 0) {
            return "0";
        }
        while (l > 0) {
            char c = (char) (l % (26));
            char o;
            c += 'a';
            o = c;
            out.append(o);
            l /= 26;
        }
        return out.toString();
    }

    /**
     * utility function for doing a base64 of some bytes. URL safe, converts
     * '=/+' to ',._' respectively.
     *
     * @param b
     *            some data in bytes
     * @return string
     */
    public static String cheapEncode(byte b[]) {
        StringBuffer sb = new StringBuffer(DatatypeConverter.printBase64Binary(b));
        char c;
        for (int i = 0; i < sb.length() && ((c = sb.charAt(i)) != '='); i++) {
            /* if (c == '=') {
                sb.setCharAt(i, ',');
            } else */if (c == '/') {
                sb.setCharAt(i, '.');
            } else if (c == '+') {
                sb.setCharAt(i, '_');
            }
        }

        return sb.toString();
    }

    static final Charset utf8 = Charset.forName("UTF-8");

    public static String cheapEncodeString(String s) {
        StringBuffer sb = new StringBuffer(DatatypeConverter.printBase64Binary(s.getBytes(utf8)));
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '=') {
                sb.setCharAt(i, ',');
            } else if (c == '/') {
                sb.setCharAt(i, '.');
            } else if (c == '+') {
                sb.setCharAt(i, '_');
            }
        }
        return sb.toString();
    }

    public static String cheapDecodeString(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == ',') {
                sb.setCharAt(i, '=');
            } else if (c == '.') {
                sb.setCharAt(i, '/');
            } else if (c == '_') {
                sb.setCharAt(i, '+');
            }
        }
        byte[] b = DatatypeConverter.parseBase64Binary(sb.toString());
        return new String(b, utf8);
    }

    /**
     * Set these from cldr.properties.
     *
     * @author srl
     *
     */
    public enum Params {
        CLDR_MAX_USERS(30), // max logged in user count allowed
        CLDR_MAX_GUESTS(0), // max guests allowed before guests start getting shut out (should be < CLDR_MAX_USERS)
        CLDR_GUEST_TIMEOUT(1 * 60), // Guest computers must checkin every minute or they are kicked. (always)
        CLDR_GUEST_INACTIVITY(1 * 60), // Guests must perform some activity every 5 minutes or they are kicked ( when too many guests)
        CLDR_USER_TIMEOUT(2 * 60), // Users computer must check in every 2 minutes or kicked (always)
        CLDR_USER_INACTIVITY(5 * 60); // Users must do something (load a page, vote, etc) or they are kicked (when too many users)

        private int defVal;
        private Integer value;

        /**
         * Construct a new parameter
         * @param name
         * @param defVal
         */
        Params(int defVal) {
            this.defVal = defVal;
        }

        /**
         * Get the param's value
         * @return
         */
        final public int value() {
            if (value == null) {
                value = CLDRConfig.getInstance().getProperty(this.name().toUpperCase(), defVal);
                SurveyLog.warnOnce("CookieSession: " + this.name().toUpperCase() + "=" + value + " (default: " + defVal + ")");
            }
            return value;
        }
    };

    /**
     * Return true if too many users are logged in.
     * @return
     */
    public static boolean tooManyUsers() {
        int limit = Params.CLDR_MAX_USERS.value();
        if (limit == 0) {
            return false;
        } else {
            int count = CookieSession.getUserCount();
            if (count >= limit) {
                if (SurveyMain.isUnofficial()) {
                    System.out.println("Too many users. limit=" + limit + ", count=" + count);
                }
                return true;
            } else {
                if (false && DEBUG_INOUT && SurveyMain.isUnofficial()) {
                    System.out.println("User count OK. . limit=" + limit + ", count=" + count);
                }
                return false;
            }
        }
    }

    /**
     * Return true if too many guests are logged in.
     * @return
     */
    public static boolean tooManyGuests() {
        int limit = Params.CLDR_MAX_GUESTS.value();
        if (tooManyUsers()) {
            return true; // implies too many guests..
        } else if (limit == 0) {
            return false;
        } else {
            int count = CookieSession.getGuestCount();
            if (count >= limit) {
                if (SurveyMain.isUnofficial())
                    System.out.println("TOO MANY GUESTS. limit=" + limit + ", count=" + count);
                return true;
            } else {
                if (false && DEBUG_INOUT && SurveyMain.isUnofficial())
                    System.out.println("Guest count OK. . limit=" + limit + ", count=" + count);
                return false;
            }
        }
    }

    // parameters

    static long lastReap = 0;
    /** last time reaped. Starts at 0, so reap immediately **/

    private static int nGuests = 0;
    /** # of guests **/
    private static int nUsers = 0;

    public static int getGuestCount() {
        getUserCount();
        return nGuests;
    }

    /** # of users **/
    /**
     * Perform a reap if need be, and count.
     * @return user count
     */
    public static int getUserCount() {
        synchronized (uHash) {
            return uHash.size();
        }
    }

    private static int lastCount = -1;

    public static int checkForExpiredSessions() {

        synchronized (gHash) {
            int allCount = gHash.size(); // count of ALL users
            long now = System.currentTimeMillis();
            long elapsed = (now - lastReap);

            final boolean tooManyUsers = tooManyUsers();
            final boolean tooManyGuests = tooManyGuests();

            if (elapsed < (1000 * 5) && allCount <= lastCount && !tooManyGuests) { // check every 5 seconds or if count grows
                return nUsers;
            }

            lastCount = allCount;

            int guests = 0;
            int users = 0;
            lastReap = now;

            // remove any sessions we need to get rid of, count the rest.
            List<CookieSession> toRemove = new LinkedList<CookieSession>();
            for (CookieSession cs : gHash.values()) {
                if (cs.user == null) { // guest
                    if (tooManyUsers || cs.age() > (Params.CLDR_GUEST_TIMEOUT.value() * 1000) || (cs.timeTillKick() == 0)) {
                        toRemove.add(cs);
                    } else {
                        guests++;
                    }
                } else {
                    if ((cs.age() > Params.CLDR_USER_TIMEOUT.value() * 1000) || (cs.timeTillKick() <= 0)) {
                        toRemove.add(cs);
                    } else {
                        users++;
                    }
                }
            }
            for (CookieSession cs : toRemove) {
                if (SurveyMain.isUnofficial()) {
                    System.err.println("Removed stale session " + cs);
                }
                cs.remove();
            }
            nGuests = guests;
            return (nUsers = users);
        }
    }

    public static void shutdownDB() {
        synchronized (gHash) {
            CookieSession sessions[] = gHash.values().toArray(new CookieSession[0]);
            for (CookieSession cs : sessions) {
                try {
                    cs.remove();
                } catch (Throwable t) {
                    //
                }
            }
            gHash.clear();
            uHash.clear();
        }
    }

    public UserSettings settings() {
        if (settings == null) {
            if (user == null) {
                settings = new EphemeralSettings();
            } else {
                settings = user.settings();
            }
        }
        return settings;
    }

    private UserSettings settings;

    static CookieSession specialGuest = null;

    private static synchronized CookieSession getSpecialGuest() {
        if (specialGuest == null) {
            specialGuest = new CookieSession(true, "[throttled]", null);
            // gHash.put("throttled", specialGuest);
        }
        return specialGuest;
    }

    private static class BadUserRecord {
        String ip;
        int hits = 0;
        Set<String> agents = new HashSet<String>();

        public BadUserRecord(String IP) {
            ip = IP;
        }

        public void hit(String userAgent) {
            agents.add(userAgent);
            hits++;
        }

        public String toString() {
            String s = " hits: " + hits + ", from :";
            for (String ua : agents) {
                s = s + ua + ", ";
            }
            return s;
        }
    }

    public static synchronized CookieSession checkForAbuseFrom(String userIP, Hashtable<String, Object> BAD_IPS, String userAgent) {
        if (userAgent == null)
            userAgent = "X-None";
        if (BAD_IPS.containsKey(userIP)) {
            BadUserRecord bur = (BadUserRecord) BAD_IPS.get(userIP);
            bur.hit(userAgent);
            return getSpecialGuest();
        }

        if (SurveyMain.isUnofficial()) {
            return null; // OK.
        }

        // get the # of sessions

        int noSes = 0;
        long now = System.currentTimeMillis();
        synchronized (gHash) {
            for (Object o : gHash.values()) {
                CookieSession cs = (CookieSession) o;
                if (!userIP.equals(cs.ip)) {
                    continue;
                }
                if (cs.user != null) {
                    return null; // has a user, OK
                }
                if ((now - cs.last) < (5 * 60 * 1000)) {
                    noSes++;
                }
            }
        }
        if ((noSes > 10) || userAgent.contains("Googlebot") || userAgent.contains("MJ12bot") || userAgent.contains("ezooms.bot")
            || userAgent.contains("bingbot")) {
            // System.err.println(userIP+" has " + noSes +
            // " sessions recently.");
            BadUserRecord bur = new BadUserRecord(userIP);
            bur.hit(userAgent);

            BAD_IPS.put(userIP, bur);
            return getSpecialGuest();
        } else {
            return null; // OK.
        }
    }

    public String banIn(Hashtable<String, Object> BAD_IPS) {
        synchronized (gHash) {
            BadUserRecord bur = (BadUserRecord) BAD_IPS.get(this.ip);
            if (bur == null) {
                bur = new BadUserRecord(this.ip);
                BAD_IPS.put(this.ip, bur);
            } else {
                bur.hit("(Banned by Admin)");
            }
            this.remove();
            return "banned and kicked this session";
        }
    }

    /**
     * User's organization or null.
     *
     * @return
     */
    public String getUserOrg() {
        if (user != null) {
            return user.org;
        } else {
            return null;
        }
    }

    /**
     * Get the coverage level for my organization (if I have one)
     * @param locale
     * @return
     */
    public String getOrgCoverageLevel(String locale) {
        String level;
        String myOrg = getUserOrg();
        if ((myOrg == null) || !WebContext.isCoverageOrganization(myOrg)) {
            level = WebContext.COVLEV_DEFAULT_RECOMMENDED_STRING;
        } else {
            org.unicode.cldr.util.Level l = StandardCodes.make().getLocaleCoverageLevel(myOrg, locale);
            if (l == Level.UNDETERMINED) {
                l = WebContext.COVLEVEL_DEFAULT_RECOMMENDED;
            }
            level = l.toString();
        }
        return level;
    }

    /**
     * Get my actual effective coverage level, including preference settings.
     * @param locale
     * @return
     */
    public String getEffectiveCoverageLevel(String locale) {
        String level = sm.getListSetting(settings, SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST, false);
        if ((level == null) || (level.equals(WebContext.COVLEV_RECOMMENDED)) || (level.equals("default"))) {
            // fetch from org
            level = getOrgCoverageLevel(locale);
        }
        return level;
    }
}
