/* Copyright (C) 2005-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               */
//
//  CookieSession.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/17/2005.
//

package org.unicode.cldr.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.UserRegistry.User;

/**
 * Instances of this class represent the session-persistent data kept on a per-user basis. Instances
 * are typically held by WebContext.session.
 */
public class CookieSession {
    static final Logger logger = SurveyLog.forClass(CookieSession.class);
    /*
     * If KICK_IF_INACTIVE is true, then we may disconnect a user if they don't perform
     * an "active" action, triggering userDidAction(), within some length of time.
     *
     * If KICK_IF_ABSENT is true, then we may disconnect a user if there is no
     * client-server communication (such as "SurveyAjax?what=status", which happens
     * automatically) within some length of time.
     *
     * At least one of KICK_IF_INACTIVE and KICK_IF_ABSENT should be true.
     * Currently we want KICK_IF_INACTIVE true and KICK_IF_ABSENT false. Actually when
     * KICK_IF_INACTIVE is true, KICK_IF_ABSENT would make no difference unless there
     * were a shorter time-out for absence than for inactivity.
     *
     * Details: There are two ways to time-out: cs.millisSinceLastBrowserCall() and cs.millisTillKick().
     *  millisSinceLastBrowserCall is based on lastBrowerCallMillisSinceEpoch, which is "last time we heard from their browser, at all"
     *     -- depends on KICK_IF_ABSENT
     *  millisTillKick is based on lastActionMillisSinceEpoch, which is "last active action (last voted, viewed a page, etc)"
     *     -- depends on KICK_IF_INACTIVE
     */
    private static final boolean KICK_IF_INACTIVE = true;
    private static final boolean KICK_IF_ABSENT = false;

    static final boolean DEBUG_INOUT = false;
    public String id;
    public String ip;

    /**
     * "stuff" must be public since it is referenced by jsp for bulk upload Reference:
     * https://unicode-org.atlassian.net/browse/CLDR-15676
     */
    public Hashtable<String, Object> stuff = new Hashtable<>(); // user data

    public Hashtable<String, Comparable> prefs = new Hashtable<>(); // user prefs
    public UserRegistry.User user = null;
    /**
     * CookieSession.sm was formerly deprecated: "need to refactor anything that uses this." But,
     * refactor how?? One possibility: "sm = SurveyMain.getInstance(request)" as in
     * SurveyAjax.processRequest, which requires the HttpServletRequest. Another possibility:
     * WebContext.sm (not static; e.g. "ctx.sm") Those aren't always possible or straightforward.
     * The huge number of deprecation warnings produced by this deprecation were distractions.
     */
    public static SurveyMain sm = null;

    /**
     * The time (in millis since 1970) when the user last took an explicit action.
     *
     * <p>Compare lastBrowserCallMillisSinceEpoch.
     */
    private long lastActionMillisSinceEpoch = System.currentTimeMillis();

    /**
     * Get the time (in millis since 1970) when the user last took an explicit action.
     *
     * @return the time
     *     <p>Called only by AdminAjax.jsp.
     *     <p>Compare getLastBrowserCallMillisSinceEpoch.
     */
    public long getLastActionMillisSinceEpoch() {
        return lastActionMillisSinceEpoch;
    }

    /**
     * How long, in millis, before we kick due to inactivity?
     *
     * @return the number of milliseconds remaining before the user will be disconnected unless the
     *     user does something "active" before then.
     *     <p>Here, something "active" means anything that causes userDidAction() to be called.
     *     <p>Called locally and by SurveyAjax.processRequest
     */
    public long millisTillKick() {
        if (!KICK_IF_INACTIVE) {
            return 1000000; // anything more than one minute, to prevent browser console countdown
        }
        final long nowMillisSinceEpoch = System.currentTimeMillis();
        final boolean observer = (user == null);
        long myTimeoutSecs; // timeout in seconds.

        if (observer) {
            myTimeoutSecs = Params.CLDR_OBSERVER_TIMEOUT_SECS.value();
            /*
             * Allow twice as much time, if there aren't too many observers.
             */
            if (!tooManyObservers()) {
                myTimeoutSecs *= 2;
            }
        } else {
            myTimeoutSecs = Params.CLDR_USER_TIMEOUT_SECS.value();
            /*
             * Allow twice as much time, if there aren't too many users.
             */
            if (!tooManyUsers()) {
                myTimeoutSecs *= 2;
            }
        }

        final long remainMillis =
                (1000 * myTimeoutSecs) - (nowMillisSinceEpoch - lastActionMillisSinceEpoch);

        if (remainMillis < 0) {
            return 0;
        } else {
            return remainMillis;
        }
    }

    /**
     * The time (in millis since 1970) when the user last touched this session.
     *
     * <p>Set only by touch(); returned by getLastBrowserCallMillisSinceEpoch().
     *
     * <p>Compare lastActionMillisSinceEpoch.
     */
    private long lastBrowserCallMillisSinceEpoch;

    /**
     * Get the time (in millis since 1970) when the user last touched this session.
     *
     * <p>Compare getLastActionMillisSinceEpoch.
     *
     * <p>Called by SurveyMain and AdminAjax.jsp.
     */
    public long getLastBrowserCallMillisSinceEpoch() {
        return lastBrowserCallMillisSinceEpoch;
    }

    /**
     * TODO: clarify who calls this and why; the usage of durationDiff with millisTillKick appears
     * dubious
     */
    @Override
    public String toString() {
        return "{CookieSession#"
                + id
                + ", user="
                + user
                + ", millisTillKick="
                + SurveyMain.durationDiff(millisTillKick())
                + ", millisSinceLastBrowserCall="
                + millisSinceLastBrowserCall()
                + ", millisSinceLastUserAction="
                + millisSinceLastUserAction()
                + "}";
    }

    static Hashtable<String, CookieSession> gHash = new Hashtable<>(); // hash by sess ID
    static Hashtable<String, CookieSession> uHash = new Hashtable<>(); // hash by user ID

    /**
     * @return the set of CookieSession objects Called by AdminAjax.jsp
     */
    public static Set<CookieSession> getAllSet() {
        synchronized (gHash) {
            TreeSet<CookieSession> sessSet =
                    new TreeSet<>(
                            (Comparator<Object>)
                                    (a, b) -> {
                                        CookieSession aa = (CookieSession) a;
                                        CookieSession bb = (CookieSession) b;
                                        if (aa == bb) return 0;
                                        return Long.compare(
                                                bb.lastBrowserCallMillisSinceEpoch,
                                                aa.lastBrowserCallMillisSinceEpoch);
                                        // same age
                                    });
            sessSet.addAll(gHash.values()); // ALL sessions
            return sessSet;
        }
    }

    /**
     * Fetch a specific session. 'touch' it (mark it as recently active) if found.
     *
     * @return session or null
     * @param sessionid id to fetch
     */
    public static CookieSession retrieve(String sessionid) {
        if (sessionid == null || sessionid.isEmpty()) {
            return null;
        }
        CookieSession c = retrieveWithoutTouch(sessionid);
        if (c != null) {
            c.touch();
        }
        return c;
    }

    /**
     * fetch a session if it exists. Don't touch it as recently active. Useful for administratively
     * retrieving a session
     *
     * @param sessionid session ID
     * @return session or null
     */
    public static CookieSession retrieveWithoutTouch(String sessionid) {
        checkForExpiredSessions();
        synchronized (gHash) {
            return gHash.get(sessionid);
        }
    }

    /**
     * Retrieve a user's session. Don't touch it (mark it active) if found.
     *
     * @param email user's email
     * @return session or null
     */
    public static CookieSession retrieveUserWithoutTouch(String email) {
        synchronized (gHash) {
            return uHash.get(email);
        }
    }

    /**
     * Retrieve a user's session. Touch it (mark it active) if found.
     *
     * @param email user's email
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

    /** only for tests. */
    public static CookieSession getTestSession(User user) {
        final CookieSession extant = retrieveUser(user);
        if (extant != null) {
            return extant;
        }
        return newSession(user, "TEST.TEST.TEST.TEST");
    }

    /**
     * Retrieve the session for a user
     *
     * @param user
     * @return
     */
    public static CookieSession retrieveUser(User user) {
        return retrieveUser(user.email);
    }

    /**
     * Create a new session for this user. Will return an existing valid session if one is
     * available.
     *
     * @param user the user to create
     * @param ip the user's IP
     * @return
     */
    public static CookieSession newSession(User user, final String ip) {
        CookieSession session = CookieSession.newSession(ip);
        session.setUser(user);
        return session;
    }

    /**
     * Associate this session with a user
     *
     * @param u user
     */
    public void setUser(UserRegistry.User u) {
        if (u == null) return;
        user = u;
        settings = null;
        synchronized (gHash) {
            uHash.put(user.email, this); // replaces any existing session by
            // this user.
        }
    }

    /** Create a new session. */
    private CookieSession(String ip, String fromId) {
        this.ip = ip;
        if (fromId == null) {
            id = newId();
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

    public static CookieSession newSession(String ip) {
        return newSession(ip, CookieSession.newId());
    }

    public static CookieSession newSession(String ip, String fromId) {
        CookieSession rv;
        synchronized (gHash) {
            rv = gHash.get(fromId);
            if (rv != null) {
                System.err.println("Trying to create extant session " + rv);
                if (!rv.ip.equals(ip)) {
                    if (SurveyMain.isUnofficial())
                        System.out.println("IP changed from " + rv.ip + " to " + ip + " - " + rv);
                    rv.ip = ip;
                    rv.touch();
                }
            } else {
                rv = new CookieSession(ip, fromId);
            }
        }
        return rv;
    }

    /** mark this session as recently updated and shouldn't expire */
    public void touch() {
        lastBrowserCallMillisSinceEpoch = System.currentTimeMillis();
        if (DEBUG_INOUT) System.out.println("S: touch " + id + " - " + user);
    }

    /** Note a direct user action. */
    public void userDidAction() {
        lastActionMillisSinceEpoch = System.currentTimeMillis();
        // if (user != null) {
        //     user.touch(); // explicitly update user last login time
        // }
    }

    /**
     * Delete a session.
     *
     * @return the user that was deleted, if any
     */
    public UserRegistry.User remove() {
        synchronized (gHash) {
            if (user != null) {
                uHash.remove(user.email);
            }
            gHash.remove(id);
        }
        if (DEBUG_INOUT) System.out.println("S: Removing session: " + id + " - " + user);
        return user;
    }

    /**
     * Remove a specific session (and return if found)
     *
     * @param sessionId
     * @return the user that was logged out, if any
     */
    public static UserRegistry.User remove(String sessionId) {
        CookieSession sess = CookieSession.retrieveWithoutTouch(sessionId);
        if (sess != null) {
            return sess.remove(); // forcibly remove session
        }
        return null;
    }

    /**
     * How long has it been since last time we heard from the user's browser, at all
     *
     * @return age since the last browser call, in millis
     */
    private long millisSinceLastBrowserCall() {
        return (System.currentTimeMillis() - lastBrowserCallMillisSinceEpoch);
    }

    /**
     * How long has it been since the user's last active action? (last voted, viewed a page, etc)
     *
     * @return age since the user's last active action, in millis
     */
    private long millisSinceLastUserAction() {
        return (System.currentTimeMillis() - lastActionMillisSinceEpoch);
    }

    // secure stuff
    static SecureRandom myRand = null;

    /* Secure random number generator */

    /** Generate a new ID. */
    public static synchronized String newId() {
        try {
            if (myRand == null) {
                myRand = SecureRandom.getInstance("SHA1PRNG");
            }

            MessageDigest aDigest = MessageDigest.getInstance("SHA-1");
            byte[] outBytes = aDigest.digest(Integer.toString(myRand.nextInt()).getBytes());
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
     * @param key the key to load
     */
    Object get(String key) {
        synchronized (stuff) {
            return stuff.get(key);
        }
    }

    /**
     * Store an object in the session
     *
     * @param key the key to set
     * @param value object to be set
     */
    public void put(String key, Object value) {
        synchronized (stuff) {
            stuff.put(key, value);
        }
    }

    /**
     * Fetch a named boolean from the preferences string
     *
     * @param key parameter to look at
     * @param defVal default value of parameter
     * @return boolean result, or defVal as default
     */
    boolean prefGetBool(String key, boolean defVal) {
        Boolean b = (Boolean) prefs.get(key);
        if (b == null) {
            return defVal;
        } else {
            return b;
        }
    }

    /**
     * Get a string preference
     *
     * @param key the key to load
     * @return named pref, or null as default
     */
    String prefGet(String key) {
        return (String) prefs.get(key);
    }

    /**
     * Store a boolean preference value
     *
     * @param key the pref to put
     * @param value boolean value
     */
    void prefPut(String key, boolean value) {
        prefs.put(key, value);
    }

    /**
     * Store a string preference value
     *
     * @param key the pref to put
     * @param value string value
     */
    void prefPut(String key, String value) {
        prefs.put(key, value);
    }

    /**
     * Fetch a hashtable of per-locale session data. Will create one if it wasn't already there.
     *
     * @return the locale hashtable
     */
    public Hashtable<String, Hashtable<String, Object>> getLocales() {
        synchronized (stuff) {
            Hashtable<String, Hashtable<String, Object>> l =
                    (Hashtable<String, Hashtable<String, Object>>) get("locales");
            if (l == null) {
                l = new Hashtable<>();
                put("locales", l);
            }
            return l;
        }
    }

    /**
     * utility function for doing an encoding of a long. Creates a base26 (a-z) representation of
     * the number, plus a leading '-' if negative. TODO: shrink? base64?
     *
     * @param l some number
     * @return string
     */
    public static String cheapEncode(long l) {
        StringBuilder out = new StringBuilder(10);
        if (l < 0) {
            out.append("-");
            l = -l;
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

    public static String cheapEncode(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String cheapEncodeString(String s) {
        return cheapEncode(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String cheapDecodeString(String s) {
        return new String(cheapDecode(s), StandardCharsets.UTF_8);
    }

    public static byte[] cheapDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    /**
     * Parameters for when to disconnect users. These defaults can be changed in cldr.properties.
     * Formerly CLDR_USER_TIMEOUT only related to what's now called KICK_IF_ABSENT, while
     * CLDR_USER_INACTIVITY only related to what's now called KICK_IF_INACTIVE. No more distinction
     * _INACTIVITY versus _TIMEOUT, use _TIMEOUT_SECS for all. Reference:
     * https://unicode-org.atlassian.net/browse/CLDR-11799
     *
     * @author srl
     */
    private enum Params {
        /** max logged in user count allowed before stricter requirements apply to stay connected */
        CLDR_MAX_USERS(30),

        /**
         * max observers allowed before observers start getting shut out (should be <
         * CLDR_MAX_USERS)
         */
        CLDR_MAX_OBSERVERS(0), // zero means tooManyObservers == tooManyUsers

        /** how many seconds observer can be absent/inactive before getting kicked */
        CLDR_OBSERVER_TIMEOUT_SECS(5 * 60), // 5 minutes

        /** how many seconds logged-in user can be absent/inactive before getting kicked */
        CLDR_USER_TIMEOUT_SECS(30 * 60); // 30 minutes

        private final int defVal;
        private Integer value;

        /**
         * Construct a new parameter
         *
         * @param defVal
         */
        Params(int defVal) {
            this.defVal = defVal;
        }

        /**
         * Get the param's value
         *
         * @return
         */
        public final int value() {
            if (value == null) {
                value = CLDRConfig.getInstance().getProperty(this.name().toUpperCase(), defVal);
                SurveyLog.warnOnce(
                        logger,
                        "CookieSession: "
                                + this.name().toUpperCase()
                                + "="
                                + value
                                + " (default: "
                                + defVal
                                + ")");
            }
            return value;
        }
    }

    /**
     * Return true if too many users are logged in.
     *
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
     * Return true if too many observers are logged in.
     *
     * @return
     */
    public static boolean tooManyObservers() {
        int limit = Params.CLDR_MAX_OBSERVERS.value();
        if (tooManyUsers()) {
            return true; // implies too many observers..
        } else if (limit == 0) {
            return false;
        } else {
            int count = CookieSession.getObserverCount();
            if (count >= limit) {
                if (SurveyMain.isUnofficial())
                    System.out.println("TOO MANY OBSERVERS. limit=" + limit + ", count=" + count);
                return true;
            } else {
                if (false && DEBUG_INOUT && SurveyMain.isUnofficial())
                    System.out.println("Oberver count OK. . limit=" + limit + ", count=" + count);
                return false;
            }
        }
    }

    // parameters

    /** last time reaped. Starts at 0, so reap immediately */
    static long lastReapMillisSinceEpoch = 0;

    /** Number of observers (users who are not logged in) */
    private static int nObservers = 0;

    /** Number of users */
    private static int nUsers = 0;

    public static int getObserverCount() {
        getUserCount();
        return nObservers;
    }

    /**
     * Perform a reap if need be, and count.
     *
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
            long nowMillisSinceEpoch = System.currentTimeMillis();
            long elapsedMillis = (nowMillisSinceEpoch - lastReapMillisSinceEpoch);

            final boolean tooManyUsers = tooManyUsers();
            final boolean tooManyObservers = tooManyObservers();

            final long CHECK_SECS = 5; // check every 5 seconds or if count grows
            if (elapsedMillis < (1000 * CHECK_SECS) && allCount <= lastCount && !tooManyObservers) {
                return nUsers;
            }

            lastCount = allCount;

            int observers = 0;
            int users = 0;
            lastReapMillisSinceEpoch = nowMillisSinceEpoch;

            // remove any sessions we need to get rid of, count the rest.
            List<CookieSession> toRemove = new LinkedList<>();
            for (CookieSession cs : gHash.values()) {
                if (cs.user == null) { // observer
                    if (tooManyUsers
                            || (KICK_IF_ABSENT
                                    && cs.millisSinceLastBrowserCall()
                                            > Params.CLDR_OBSERVER_TIMEOUT_SECS.value() * 1000L)
                            || (KICK_IF_INACTIVE && cs.millisTillKick() <= 0)) {
                        toRemove.add(cs);
                    } else {
                        observers++;
                    }
                } else {
                    if ((KICK_IF_ABSENT
                                    && cs.millisSinceLastBrowserCall()
                                            > Params.CLDR_USER_TIMEOUT_SECS.value() * 1000)
                            || (KICK_IF_INACTIVE && cs.millisTillKick() <= 0)) {
                        toRemove.add(cs);
                    } else {
                        users++;
                    }
                }
            }
            for (CookieSession cs : toRemove) {
                if (SurveyMain.isUnofficial() && cs.user != null) {
                    // Don't log on anonymous users
                    logger.fine(() -> "Removed stale session " + cs);
                }
                cs.remove();
            }
            nObservers = observers;
            return (nUsers = users);
        }
    }

    public static void shutdownDB() {
        synchronized (gHash) {
            CookieSession[] sessions = gHash.values().toArray(new CookieSession[0]);
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

    static CookieSession specialObserver = null;

    private static synchronized CookieSession getSpecialObserver() {
        if (specialObserver == null) {
            specialObserver = new CookieSession("[throttled]", null);
        }
        return specialObserver;
    }

    private static class BadUserRecord {
        String ip;
        int hits = 0;
        Set<String> agents = new HashSet<>();

        public BadUserRecord(String IP) {
            ip = IP;
        }

        public void hit(String userAgent) {
            agents.add(userAgent);
            hits++;
        }

        @Override
        public String toString() {
            String s = " hits: " + hits + ", from :";
            for (String ua : agents) {
                s = s + ua + ", ";
            }
            return s;
        }
    }

    public static synchronized CookieSession checkForAbuseFrom(
            String userIP, Hashtable<String, Object> BAD_IPS, String userAgent) {
        if (userAgent == null) userAgent = "X-None";
        if (BAD_IPS.containsKey(userIP)) {
            BadUserRecord bur = (BadUserRecord) BAD_IPS.get(userIP);
            bur.hit(userAgent);
            return getSpecialObserver();
        }

        if (SurveyMain.isUnofficial()) {
            return null; // OK.
        }

        // get the # of sessions

        int noSes = 0;
        long nowMillisSinceEpoch = System.currentTimeMillis();
        synchronized (gHash) {
            for (CookieSession cs : gHash.values()) {
                if (!userIP.equals(cs.ip)) {
                    continue;
                }
                if (cs.user != null) {
                    return null; // has a user, OK
                }
                final long N_MINUTES = 5; // five minutes (why?)
                if ((nowMillisSinceEpoch - cs.lastBrowserCallMillisSinceEpoch)
                        < (N_MINUTES * 60 * 1000)) {
                    noSes++;
                }
            }
        }
        if ((noSes > 10)
                || userAgent.contains("Googlebot")
                || userAgent.contains("MJ12bot")
                || userAgent.contains("ezooms.bot")
                || userAgent.contains("bingbot")) {
            BadUserRecord bur = new BadUserRecord(userIP);
            bur.hit(userAgent);

            BAD_IPS.put(userIP, bur);
            return getSpecialObserver();
        } else {
            return null; // OK.
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
     *
     * @param locale
     * @return
     */
    public String getOrgCoverageLevel(String locale) {
        String level;
        String myOrg = getUserOrg();
        if ((myOrg == null) || !WebContext.isCoverageOrganization(myOrg)) {
            level = WebContext.COVLEV_DEFAULT_RECOMMENDED_STRING;
        } else {
            Level coverageLevel = StandardCodes.make().getLocaleCoverageLevel(myOrg, locale);
            if (coverageLevel == Level.UNDETERMINED) {
                coverageLevel = WebContext.COVLEVEL_DEFAULT_RECOMMENDED;
            }
            level = coverageLevel.toString();
        }
        return level;
    }

    /**
     * Get my actual effective coverage level, including preference settings.
     *
     * @param locale
     * @return
     */
    public String getEffectiveCoverageLevel(String locale) {
        String level =
                sm.getListSetting(settings, SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST);
        if (level == null || level.equals(WebContext.COVLEV_RECOMMENDED)) {
            // fetch from org
            level = getOrgCoverageLevel(locale);
        }
        return level;
    }

    private String sessionMessage = null;

    public String getMessage() {
        return sessionMessage;
    }

    public void setMessage(String s) {
        sessionMessage = s;
    }
}
