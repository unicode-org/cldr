//  UserRegistry.java
//
//  Created by Steven R. Loomis on 14/10/2005.
//  Copyright 2005-2013 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LocaleNormalizer;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SpecialLocales;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

/**
 * This class represents the list of all registered users. It contains an inner
 * class, UserRegistry.User, which represents an individual user.
 *
 * @see UserRegistry.User
 **/
public abstract class UserRegistry {
    public static final String ADMIN_EMAIL = "admin@";

    /**
     * The number of anonymous users, ANONYMOUS_USER_COUNT, limits the number of distinct values that
     * can be added for a given locale and path as anonymous imported old losing votes. If eventually more
     * are needed, ANONYMOUS_USER_COUNT can be increased and more anonymous users will automatically
     * be created.
     */
    private static final int ANONYMOUS_USER_COUNT = 20;

    /**
     * Thrown to indicate the caller should log out.
     * @author srl
     */
    public class LogoutException extends Exception {
        private static final long serialVersionUID = 8960959307439428532L;
    }

    public abstract Set<String> getCovGroupsForOrg(String st_org);

    public abstract Set<CLDRLocale> anyVotesForOrg(String st_org);

    public interface UserChangedListener {
        public void handleUserChanged(User u);
    }

    private List<UserChangedListener> listeners = new LinkedList<>();

    public synchronized void addListener(UserChangedListener l) {
        listeners.add(l);
    }

    protected synchronized void notify(User u) {
        for (UserChangedListener l : listeners) {
            l.handleUserChanged(u);
        }
    }

    private static final java.util.logging.Logger logger = SurveyLog.forClass(UserRegistry.class);
    // user levels

    /**
     * Administrator
     */
    public static final int ADMIN = VoteResolver.Level.admin.getSTLevel();

    /**
     * Technical Committee
     */
    public static final int TC = VoteResolver.Level.tc.getSTLevel();

    /**
     * Manager
     */
    public static final int MANAGER = VoteResolver.Level.manager.getSTLevel();

    /**
     * Regular Vetter
     */
    public static final int VETTER = VoteResolver.Level.vetter.getSTLevel();

    /**
     * "Street" = Guest Vetter
     */
    public static final int STREET = VoteResolver.Level.street.getSTLevel();

    /**
     * Locked user - can't login
     */
    public static final int LOCKED = VoteResolver.Level.locked.getSTLevel();

    /**
     * Anonymous user - special for imported old losing votes
     */
    public static final int ANONYMOUS = VoteResolver.Level.anonymous.getSTLevel();

    /**
     * min level
     */
    public static final int NO_LEVEL = -1;

    /**
     * special "IP" value referring to a user being added
     */
    public static final String FOR_ADDING = "(for adding)";

    private static final String INTERNAL = "INTERNAL";

    /**
     * get a level as a string - presentation form
     **/
    public static String levelToStr(int level) {
        return level + ": (" + levelAsStr(level) + ")";
    }

    /**
     * get just the raw level as a string
     */
    public static String levelAsStr(int level) {
        VoteResolver.Level l = VoteResolver.Level.fromSTLevel(level);
        if (l == null) {
            return "??";
        } else {
            return l.name().toUpperCase();
        }
    }

    public abstract UserSettingsData getUserSettings();

    public UserSettings getSettings(int id) {
        return getUserSettings().getSettings(id);
    }

    /**
     * This nested class is the representation of an individual user. It may not
     * have all fields filled out, if it is simply from the cache.
     */
    public class User implements Comparable<User>, UserInfo, JSONString {
        @Schema( description = "User ID")
        public int id; // id number
        @Schema( description = "numeric userlevel")
        public int userlevel = LOCKED; // user level
        @Schema( hidden = true )
        protected String password; // password
        @Schema( description = "User email")
        public String email; //
        @Schema( description = "User org")
        public String org; // organization
        @Schema( description = "User name")
        public String name; // full name
        @Schema( name = "time", implementation = java.util.Date.class )
        public java.sql.Timestamp last_connect;
        @Schema( hidden = true )
        public String locales;
        @Schema( hidden = true )
        public String intlocs = null;
        @Schema( hidden = true )
        public String ip;

        @Schema( hidden = true )
        private String emailmd5 = null;

        public String getEmailHash() {
            if (emailmd5 == null) {
                String newHash = DigestUtils.md5Hex(email.trim().toLowerCase());
                emailmd5 = newHash;
                return newHash;
            }
            return emailmd5;
        }

        private UserSettings settings;

        private LocaleSet authorizedLocaleSet = null;
        private LocaleSet interestLocalesSet = null;

        /**
         * @deprecated may not use
         */
        @Deprecated
        private User() {
            this.id = UserRegistry.NO_USER;
            settings = getSettings(id); // may not use settings.
        }

        public User(int id) {
            this.id = id;
            settings = getSettings(id);
        }

        /**
         * Get a settings object for use with this user.
         *
         * @return
         */
        public UserSettings settings() {
            return settings;
        }

        public void touch() {
            UserRegistry.this.touch(id);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof User)) {
                return false;
            }
            User u = (User) other;
            return (u.id == id);
        }

        public void printPasswordLink(WebContext ctx) {
            UserRegistry.printPasswordLink(ctx, email, getPassword());
        }

        @Override
        public String toString() {
            return email + "(" + org + ")-" + levelAsStr(userlevel) + "#" + userlevel + " - " + name + ", locs=" + locales;
        }

        public String toHtml(User forUser) {
            if (forUser == null || !userIsTC(forUser)) {
                return "(" + org + "#" + id + ")";
            } else {
                return "<a href='mailto:" + email + "'>" + name + "</a>-" + levelAsStr(userlevel).toLowerCase();
            }
        }

        public String toHtml() {
            return "<a href='mailto:" + email + "'>" + name + "</a>-" + levelAsStr(userlevel).toLowerCase();
        }

        public String toString(User forUser) {
            if (forUser == null || !userIsTC(forUser)) {
                return "(" + org + "#" + id + ")";
            } else {
                return email + "(" + org + ")-" + levelAsStr(userlevel) + "#" + userlevel + " - " + name;
            }
        }

        @Override
        public int hashCode() {
            return id;
        }

        /**
         * is the user interested in this locale?
         */
        public boolean interestedIn(CLDRLocale locale) {
            return getInterestLocales().contains(locale);
        }

        /**
         * Set of interest locales for this user
         *
         * @return null or LocaleNormalizer.ALL_LOCALES_SET for 'all', otherwise a set of CLDRLocales
         */
        public LocaleSet getInterestLocales() {
            if (interestLocalesSet == null) {
                if (userIsManagerOrStronger(this)) {
                    interestLocalesSet = LocaleNormalizer.setFromStringQuietly(intlocs, null);
                } else {
                    interestLocalesSet = LocaleNormalizer.setFromStringQuietly(locales, null);
                }
            }
            return interestLocalesSet;
        }

        /**
         * Convert this User to a VoteResolver.VoterInfo. Not cached.
         */
        VoterInfo createVoterInfo() {
            Organization o = this.getOrganization();
            VoteResolver.Level l = this.getLevel();
            VoterInfo v = new VoterInfo(o, l,
                // do not allow VoteResolver.VoterInfo to see the actual "name", because it is sent to the client.
                "#"+Integer.toString(id),
                getAuthorizedLocaleSet());
            return v;
        }

        private LocaleSet getAuthorizedLocaleSet() {
            if (authorizedLocaleSet == null) {
                LocaleSet orgLocales = getOrganization().getCoveredLocales();
                authorizedLocaleSet = LocaleNormalizer.setFromStringQuietly(locales, orgLocales);
            }
            return authorizedLocaleSet;
        }

        @Override
        public VoterInfo getVoterInfo() {
            return getVoterToInfo(id);
        }

        @Schema( name = "userLevelName", description = "VoteREsolver.Level user level" )
        public synchronized VoteResolver.Level getLevel() {
            return VoteResolver.Level.fromSTLevel(this.userlevel);
        }

        String getPassword() {
            return password;
        }

        /** here to allow one JSP to get at the password, but otherwise keep the field hidden */
        @Deprecated
        public String internalGetPassword() {
            return getPassword();
        }

        public synchronized Organization getOrganization() {
            if (vr_org == null) {
                vr_org = UserRegistry.computeVROrganization(this.org);
            }
            return vr_org;
        }

        private Organization vr_org = null;

        private String voterOrg = null;

        /**
         * Convenience function for returning the "VoteResult friendly"
         * organization.
         */
        public String voterOrg() {
            if (voterOrg == null) {
                voterOrg = getVoterInfo().getOrganization().name();
            }
            return voterOrg;
        }

        /**
         * Is this user an administrator 'over' the given other user?
         *
         * @param other
         * @deprecated
         *
         * Maybe this should NOT be deprecated, since it's shorter and more convenient
         * than VoteResolver.Level.isManagerFor()
         */
        @Deprecated
        public boolean isAdminFor(User other) {
            return getLevel().isManagerFor(getOrganization(), other.getLevel(), other.getOrganization());
        }

        public boolean isSameOrg(User other) {
            return getOrganization() == other.getOrganization();
        }

        /**
         * Is this user an administrator 'over' this user? Always true if admin,
         * or if TC in same org.
         *
         * @param org
         */
        public boolean isAdminForOrg(String org) {
            return getLevel().isAdminForOrg(getOrganization(), Organization.fromString(org));
        }

        @Override
        public int compareTo(User other) {
            if (other == this || other.equals(this))
                return 0;
            if (this.id < other.id) {
                return -1;
            } else {
                return 1;
            }
        }

        public Organization vrOrg() {
            return Organization.fromString(voterOrg());
        }

        /**
         * Doesn't send the password, does send other info
         * TODO: remove this in favor of jax-rs serialization
         */
        @Override
        public String toJSONString() throws JSONException {
            return new JSONObject().put("email", email)
                .put("emailHash", getEmailHash())
                .put("name", name)
                .put("userlevel", userlevel)
                .put("votecount", getLevel().getVotes(getOrganization()))
                .put("voteCountMenu", getLevel().getVoteCountMenu(getOrganization()))
                .put("userlevelName", UserRegistry.levelAsStr(userlevel))
                .put("org", vrOrg().name())
                .put("orgName", vrOrg().displayName)
                .put("id", id)
                .toString();
        }

        public boolean canImportOldVotes() {
            return canImportOldVotes(CLDRConfig.getInstance().getPhase());
        }

        public boolean canImportOldVotes(CheckCLDR.Phase inPhase) {
            return getLevel().canImportOldVotes(inPhase);
        }

        @Schema( description="how much this user’s vote counts for")
        // @JsonbProperty("votecount")
        public int getVoteCount() {
            return getLevel().getVotes(getOrganization());
        }

        @Schema( description="how much this user’s vote counts for")
        // @JsonbProperty("voteCountMenu")
        public Integer[] getVoteCountMenu() {
            return getLevel().getVoteCountMenu(getOrganization()).toArray(new Integer[0]);
        }

        /**
         * This one is hidden because it uses JSONObject and can't be serialized
         */
        JSONObject getPermissionsJson() throws JSONException {
            return new JSONObject()
                .put("userCanImportOldVotes", canImportOldVotes())
                .put("userCanUseVettingSummary", userCanUseVettingSummary(this))
                .put("userCanCreateSummarySnapshot", userCanCreateSummarySnapshot(this))
                .put("userCanMonitorForum", userCanMonitorForum(this))
                .put("userIsAdmin", userIsAdmin(this))
                .put("userIsManager", getLevel().canManageSomeUsers())
                .put("userIsTC", userIsTC(this))
                .put("userIsVetter", userIsVetter(this) && !userIsTC(this))
                .put("userIsLocked", userIsLocked(this));
        }

        /**
         * this property is called permissionsJson for compatiblity
         * @return
         */
        @JsonbProperty( "permissionsJson" )
        @Schema( description = "array of permissions for this user" )
        public Map<String, Boolean> getPermissions() {
            Map<String, Boolean> m = new HashMap<>();

            m.put("userCanImportOldVotes", canImportOldVotes());
            m.put("userCanUseVettingSummary", userCanUseVettingSummary(this));
            m.put("userCanCreateSummarySnapshot", userCanCreateSummarySnapshot(this));
            m.put("userCanMonitorForum", userCanMonitorForum(this));
            m.put("userIsAdmin", userIsAdmin(this));
            m.put("userIsTC", userIsTC(this));
            m.put("userIsManager", getLevel().canManageSomeUsers());
            m.put("userIsVetter", userIsVetter(this) && !userIsTC(this));
            m.put("userIsLocked", userIsLocked(this));

            return m;
        }

        public void setPassword(String randomPass) {
            this.password = randomPass;
        }

        /**
         * Does this user have permission to modify the given locale?
         *
         * @param locale the CLDRLocale
         * @return true or false
         *
         * Code related to userIsExpert, UserRegistry.EXPERT removed 2021-05-18 per CLDR-14597
         */
        private boolean hasLocalePermission(CLDRLocale locale) {
            /*
             * the 'und' locale and sublocales can always be modified
             */
            if (SpecialLocales.getType(locale) == SpecialLocales.Type.scratch) {
                return true;
            }
            /*
             * Per CLDR-14597, TC and ADMIN can vote in any locale;
             * MANAGER can vote if their organization covers the locale
             */
            if (userlevel <= UserRegistry.TC) {
                return true;
            }
            if (userlevel == UserRegistry.MANAGER) {
                return getOrganization().getCoveredLocales().containsLocaleOrParent(locale);
            }
            return getAuthorizedLocaleSet().contains(locale);
        }

        /**
         * Get a locale suitable for example paths to be shown to this user,
         * when "USER" is used as a "wildcard" for the locale name in a URL
         *
         * @return the CLDRLocale, or null if no particularly suitable locale found
         */
        public CLDRLocale exampleLocale() {
            final LocaleSet localeSet = getAuthorizedLocaleSet();
            if (!localeSet.isEmpty() && !localeSet.isAllLocales()) {
                return localeSet.firstElement();
            }
            return null;
        }
    }

    public static void printPasswordLink(WebContext ctx, String email, String password) {
        ctx.println("<a href='" + ctx.base() + "?email=" + email + "&amp;pw=" + password + "'>Login for " + email + "</a>");
    }

    private static Map<String, Organization> orgToVrOrg = new HashMap<>();

    public static synchronized Organization computeVROrganization(String org) {
        Organization o = Organization.fromString(org);
        if (o == null) {
            o = orgToVrOrg.get(org);
        } else {
            orgToVrOrg.put(org, o); // from Organization.fromString
        }
        if (o == null) {
            try {
                /*
                 * TODO: "utilika" always logs WARNING: ** Unknown organization (treating as Guest): Utilika Foundation"
                 * Map to "The Long Now Foundation" instead? Cf. https://unicode.org/cldr/trac/ticket/6320
                 * Organization.java has: longnow("The Long Now Foundation", "Long Now", "PanLex")
                 */
                String arg = org.replaceAll("Utilika Foundation", "utilika")
                    .replaceAll("Government of Pakistan - National Language Authority", "pakistan")
                    .replaceAll("ICT Agency of Sri Lanka", "srilanka").toLowerCase().replaceAll("[.-]", "_");
                o = Organization.valueOf(arg);
            } catch (IllegalArgumentException iae) {
                o = Organization.guest;
                SurveyLog.warnOnce(logger, "** Unknown organization (treating as Guest): " + org);
            }
            orgToVrOrg.put(org, o);
        }
        return o;
    }

    /**
     * ID# of the user
     */
    static final int ADMIN_ID = 1;

    /**
     * special ID meaning 'all'
     */
    static final int ALL_ID = -1;


    /**
     * Mark user as modified
     *
     * @param id
     */
    public void userModified(int id) {
    }

    /**
     * Mark the UserRegistry as changed, purging the VoterInfo map
     *
     * @see #getVoterToInfo()
     */
    protected void userModified() {
    }

    /**
     * Get the singleton user for this ID.
     *
     * @param id
     * @return singleton, or null if not found/invalid
     */
    public abstract UserRegistry.User getInfo(int id);


    protected final String normalizeEmail(String str) {
        return str.trim().toLowerCase();
    }

    public final UserRegistry.User get(String pass, String email, String ip) throws LogoutException {
        boolean letmein = false;
        if (ADMIN_EMAIL.equals(email) && SurveyMain.vap.equals(pass)) {
            letmein = true;
        }
        return get(pass, email, ip, letmein);
    }

    public void touch(int id)  {
    }


    /**
     * @param letmein
     *            The VAP was given - allow the user in regardless
     * @param pass
     *            the password to match. If NULL, means just do a lookup
     */
    public abstract UserRegistry.User get(String pass, String email, String ip, boolean letmein) throws LogoutException;

    public UserRegistry.User get(String email) {
        try {
            return get(null, email, INTERNAL);
        } catch (LogoutException le) {
            return null;
        }
    }

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public UserRegistry.User getEmptyUser() {
        User u = new User();
        u.name = "UNKNOWN";
        u.email = "UN@KNOWN.example.com";
        u.org = "NONE";
        u.password = null;
        u.locales = "";
        return u;
    }

    /**
     * For test use only. Does not register in DB.
     * @param id
     * @param o
     * @param l
     * @return
     */
    UserRegistry.User getTestUser(int id, Organization o, VoteResolver.Level l) {
        User u = new User(); // Not: User(id) because that makes a DB call
        u.org = o.name();
        u.userlevel = l.getSTLevel();
        u.name = o.name() + "-" + u.getLevel().name();
        u.email = u.name + "-" + u.id + "@" + u.org + ".example.com";
        return u;
    }

    public abstract String setUserLevel(User me, User them, int newLevel);

    // TODO: should be refactored.
    public boolean canSetUserLevel(User me, User them, int newLevel) {
        final VoteResolver.Level myLevel = VoteResolver.Level.fromSTLevel(me.userlevel);
        if (!myLevel.canCreateOrSetLevelTo(VoteResolver.Level.fromSTLevel(newLevel))) {
            return false;
        }
        // Can't change your own level
        if (me.id == them.id) {
            return false;
        }
        // Can't change the level of anyone whose current level is more privileged than yours
        if (them.userlevel < me.userlevel) {
            return false;
        }
        // Can't change the level of someone in a different org, unless you are admin
        if (myLevel != Level.admin && !me.isSameOrg(them)) {
            return false;
        }
        return true;
    }

    /**
     * Set the authorized locales, or the interest locales, for the specified user
     *
     * @param session the CookieSession
     * @param user the specified user (sometimes distinct from session.user)
     * @param newLocales the set of locales, as a string like "am fr zh"
     * @param intLocs true to set interest locales, false to set authorized locales
     * @return a message string describing the result
     */
    public abstract String setLocales(CookieSession session, User user, String newLocales, boolean intLocs);

    public abstract String delete(WebContext ctx, int theirId, String theirEmail);

    public enum InfoType {
        INFO_EMAIL("E-mail", "email"), INFO_NAME("Name", "name"), INFO_PASSWORD("Password", "password"), INFO_ORG("Organization",
            "org");
        private static final String CHANGE = "change_";
        private String sqlField;
        private String title;

        InfoType(String title, String sqlField) {
            this.title = title;
            this.sqlField = sqlField;
        }

        @Override
        public String toString() {
            return title;
        }

        public String field() {
            return sqlField;
        }

        public static InfoType fromAction(String action) {
            if (action != null && action.startsWith(CHANGE)) {
                String which = action.substring(CHANGE.length());
                return InfoType.valueOf(which);
            } else {
                return null;
            }
        }

        public String toAction() {
            return CHANGE + name();
        }
    }

    abstract String updateInfo(WebContext ctx, int theirId, String theirEmail, InfoType type, String value);

    public abstract String resetPassword(String forEmail, String ip);


    /**
     * Lock (disable, unsubscribe) the specified account
     *
     * @param forEmail the E-mail address for the account
     * @param reason a string explaining the reason for locking
     * @param ip the current user's IP address
     * @return "OK" for success or other message for failure
     */
    public abstract String lockAccount(String forEmail, String reason, String ip);

    protected void userModified(String forEmail) {
        User u = get(forEmail);
        if (u != null) userModified(u);
    }

    private void userModified(User u) {
        userModified(u.id);
    }

    public abstract String getPassword(WebContext ctx, int theirId);

    public static String makePassword(String email) {
        return CookieSession.newId(false).substring(0, 9);
    }

    /**
     * Create a new user
     *
     * @param ctx - webcontext if available
     * @param u prototype - used for user information, but not for return value
     * @return the new User object for the new user, or null for failure
     */
    public abstract User newUser(WebContext ctx, User u);


    // All of the userlevel policy is concentrated here, or in above functions
    // (search for 'userlevel')

    // * user types
    public static final boolean userIsAdmin(User u) {
        return (u != null) && u.getLevel().isAdmin();
    }

    public static final boolean userIsTC(User u) {
        return (u != null) && u.getLevel().isTC();
    }

    public static final boolean userIsExactlyManager(User u) {
        return (u != null) && u.getLevel().isExactlyManager();
    }

    public static final boolean userIsManagerOrStronger(User u) {
        return (u != null) && u.getLevel().isManagerOrStronger();
    }

    public static final boolean userIsVetter(User u) {
        return (u != null) && u.getLevel().isVetter();
    }

    public static final boolean userIsStreet(User u) {
        return (u != null) && u.getLevel().isStreet();
    }

    public static final boolean userIsLocked(User u) {
        return (u != null) && u.getLevel().isLocked();
    }

    public static final boolean userIsExactlyAnonymous(User u) {
        return (u != null) && u.getLevel().isExactlyAnonymous();
    }

    // * user rights
    /** can create a user in a different organization? */
    public static final boolean userCreateOtherOrgs(User u) {
        return (u != null) && u.getLevel().canCreateOtherOrgs();
    }

    /** Can the user modify anyone's level? */
    static final boolean userCanModifyUsers(User u) {
        return (u != null) && u.getLevel().canModifyUsers();
    }

    static final boolean userCanEmailUsers(User u) {
        return (u != null) && u.getLevel().canEmailUsers();
    }

    /**
     * Returns true if the manager user can change the user's userlevel
     * @param managerUser the user doing the changing
     * @param targetId the user being changed
     * @param targetNewUserLevel the new userlevel of the user
     * @return true if the action can proceed, otherwise false
     */
    static final boolean userCanModifyUser(User managerUser, int targetId, int targetNewUserLevel) {
        if (targetId == ADMIN_ID) {
            return false; // can't modify admin user
        }
        if (managerUser == null) {
            return false; // no user
        }
        if (userIsAdmin(managerUser)) {
            return true; // admin can modify everyone
        }
        final User otherUser = CookieSession.sm.reg.getInfo(targetId); // TODO static
        if (otherUser == null) {
            return false; // ?
        }
        if (!managerUser.org.equals(otherUser.org)) {
            return false;
        }
        if (!userCanModifyUsers(managerUser)) {
            return false;
        }
        if (targetId == managerUser.id) {
            return false; // cannot modify self
        }
        if (targetNewUserLevel < managerUser.userlevel) {
            return false; // Cannot assign a userlevel higher than the manager
        }
        return true;
    }

    static final boolean userCanDeleteUser(User managerUser, int targetId, int targetLevel) {
        // must be at a lower level
        return (userCanModifyUser(managerUser, targetId, targetLevel) && targetLevel > managerUser.userlevel);
    }

    static final boolean userCanDoList(User managerUser) {
        return (managerUser != null) && managerUser.getLevel().canDoList();
    }

    public static final boolean userCanCreateUsers(User u) {
        return (u != null) && u.getLevel().canCreateUsers();
    }

    static final boolean userCanSubmit(User u) {
        return userCanSubmit(u, SurveyMain.phase());
    }

    static final boolean userCanSubmit(User u, SurveyMain.Phase phase) {
        return (u != null) && u.getLevel().canSubmit(phase.getCPhase());
    }

    /**
     * Can the user use the priority items summary page?
     *
     * @param u the user
     * @return true or false
     */
    public static final boolean userCanUseVettingSummary(User u) {
        return (u != null) && u.getLevel().canUseVettingSummary();
    }

    /**
     * Can the user create snapshots in the priority items summary page?
     *
     * @param u the user
     * @return true or false
     */
    public static boolean userCanCreateSummarySnapshot(User u) {
        return (u != null) && u.getLevel().canCreateSummarySnapshot();
    }

    /**
     * Can the user monitor forum participation?
     *
     * @param u the user
     * @return true or false
     */
    public static final boolean userCanMonitorForum(User u) {
        return (u != null) && u.getLevel().canMonitorForum();
    }

    /**
     * Can the user set their interest locales (intlocs)?
     *
     * @param u the user
     * @return true or false
     */
    public static boolean userCanSetInterestLocales(User u) {
        return (u != null) && u.getLevel().canSetInterestLocales();
    }

    /**
     * Can the user get a list of email addresses of participating users?
     *
     * @param u the user
     * @return true or false
     */
    public static boolean userCanGetEmailList(User u) {
        return (u != null) && u.getLevel().canGetEmailList();
    }

    public enum ModifyDenial {
        DENY_NULL_USER("No user specified"),
        DENY_LOCALE_READONLY("Locale is read-only"),
        DENY_PHASE_READONLY("SurveyTool is in read-only mode"),
        // DENY_ALIASLOCALE("Locale is an alias"),
        DENY_DEFAULTCONTENT("Locale is the Default Content for another locale"),
        DENY_PHASE_CLOSED("SurveyTool is in 'closed' phase"),
        DENY_NO_RIGHTS("User does not have any voting rights"),
        DENY_LOCALE_LIST("User does not have rights to vote for this locale"),
        DENY_PHASE_FINAL_TESTING("SurveyTool is in the 'final testing' phase");

        ModifyDenial(String reason) {
            this.reason = reason;
        }

        final String reason;

        public String getReason() {
            return reason;
        }
    }

    public static final boolean userCanModifyLocale(User u, CLDRLocale locale) {
        return (userCanModifyLocaleWhy(u, locale) == null);
    }

    public static final boolean userCanAccessForum(User u, CLDRLocale locale) {
        return (userCanAccessForumWhy(u, locale) == null);
    }

    private static Object userCanAccessForumWhy(User u, CLDRLocale locale) {
        if (u == null)
            return ModifyDenial.DENY_NULL_USER; // no user, no dice
        if (!userIsStreet(u))
            return ModifyDenial.DENY_NO_RIGHTS; // at least street level
        if (userIsAdmin(u))
            return null; // Admin can modify all
        if (userIsTC(u))
            return null; // TC can modify all
        if (SpecialLocales.getType(locale) == SpecialLocales.Type.scratch) {
            // All users can modify the sandbox
            return null;
        }
        if ((u.locales == null) && userIsManagerOrStronger(u))
            return null; // empty = ALL
        if (false || userIsExactlyManager(u))
            return null; // manager can edit all
        if (LocaleNormalizer.isAllLocales(u.locales)) {
            return null; // all
        }
        return userHasForumLocale(u, locale) ? null : ModifyDenial.DENY_LOCALE_LIST;
    }

    private static boolean userHasForumLocale(User u, CLDRLocale locale) {
        final CLDRLocale languageLocale = locale.getLanguageLocale();
        final LocaleSet authorizedLocaleSet = u.getAuthorizedLocaleSet();
        if (authorizedLocaleSet.isAllLocales()) {
            return true;
        }
        for (CLDRLocale l : authorizedLocaleSet.getSet()) {
            if (l.getLanguageLocale() == languageLocale) {
                return true;
            }
        }
        return false;
    }

    public static boolean countUserVoteForLocale(User theSubmitter, CLDRLocale locale) {
        return (countUserVoteForLocaleWhy(theSubmitter, locale) == null);
    }

    public static ModifyDenial countUserVoteForLocaleWhy(User u, CLDRLocale locale) {
        // must not have a null user
        if (u == null)
            return ModifyDenial.DENY_NULL_USER;

        // can't vote in a readonly locale
        if (STFactory.isReadOnlyLocale(locale))
            return ModifyDenial.DENY_LOCALE_READONLY;

        // user must have street level perms
        if (!userIsStreet(u))
            return ModifyDenial.DENY_NO_RIGHTS; // at least street level

        // locales that are default content parents can't be modified.
        CLDRLocale dcParent = CLDRConfig.getInstance().getSupplementalDataInfo().getBaseFromDefaultContent(locale);
        if (dcParent != null) {
            return ModifyDenial.DENY_DEFAULTCONTENT; // it's a defaultcontent
            // locale or a pure alias.
        }

        return u.hasLocalePermission(locale) ? null : ModifyDenial.DENY_LOCALE_LIST;
    }

    public static final ModifyDenial userCanModifyLocaleWhy(User u, CLDRLocale locale) {
        final ModifyDenial denyCountVote = countUserVoteForLocaleWhy(u, locale);

        // If we don't count the votes, modify is prohibited.
        if (denyCountVote != null) {
            return denyCountVote;
        }

        // We add more restrictions

        // Admin and TC users can always modify, even in closed state.
        if (userIsAdmin(u) || userIsTC(u))
            return null;

        // Otherwise, if closed, deny
        if (SurveyMain.isPhaseClosed())
            return ModifyDenial.DENY_PHASE_CLOSED;
        if (SurveyMain.isPhaseReadonly())
            return ModifyDenial.DENY_PHASE_READONLY;

        if (SurveyMain.isPhaseFinalTesting()) {
            return ModifyDenial.DENY_PHASE_FINAL_TESTING;
        }
        return null;
    }

    /**
     * Invalid user ID, representing NO USER.
     */
    public static final int NO_USER = -1;

    public VoterInfo getVoterToInfo(int userid) {
        return getVoterToInfo().get(userid);
    }


    // Interface for VoteResolver interface
    /**
     * Fetch the user map in VoterInfo format.
     *
     * @see #userModified()
     */
    public abstract Map<Integer, VoterInfo> getVoterToInfo();


    public abstract String[] getOrgList();

    /**
     * Cache of the set of anonymous users
     */
    private Set<User> anonymousUsers = null;

    /**
     * Get the set of anonymous users, employing a cache.
     * If there aren't as many as there should be (ANONYMOUS_USER_COUNT), create some.
     * An "anonymous user" is one whose userlevel is ANONYMOUS.
     *
     * @return the Set.
     */
    public Set<User> getAnonymousUsers() {
        if (anonymousUsers == null) {
            anonymousUsers = getAnonymousUsersFromDb();
            int existingCount = anonymousUsers.size();
            if (existingCount < ANONYMOUS_USER_COUNT) {
                createAnonymousUsers(existingCount, ANONYMOUS_USER_COUNT);
                /*
                 * After createAnonymousUsers, call userModified to clear voterInfo, so it will
                 * be reloaded and include the new anonymous users. Otherwise, we would get an
                 * "Unknown voter" exception in OrganizationToValueAndVote.add when trying to
                 * add a vote for one of the new anonymous users.
                 */
                userModified();
                anonymousUsers = getAnonymousUsersFromDb();
            }
        }
        return anonymousUsers;
    }

    /**
     * Get the set of anonymous users by running a database query.
     *
     * @return the Set.
     */
    protected abstract Set<User> getAnonymousUsersFromDb();

    /**
     * Given that there aren't enough anonymous users in the database yet, create some.
     *
     * @param existingCount the number of anonymous users that already exist
     * @param desiredCount the desired total number of anonymous users
     */
    protected abstract void createAnonymousUsers(int existingCount, int desiredCount);

    /**
     * Create a test user, for unit testing only
     *
     * @param name
     * @param org
     * @param locales
     * @param level
     * @param email
     * @return the User object, or null for failure such as for invalid locales
     */
    public User createTestUser(String name, String org, String locales, Level level, String email) {
        final LocaleNormalizer locNorm = new LocaleNormalizer();
        final Organization organization = Organization.fromString(org);
        final String normLocales = locNorm.normalizeForSubset(locales, organization.getCoveredLocales());
        if (locNorm.hasMessage()) {
            logger.log(java.util.logging.Level.SEVERE, locNorm.getMessagePlain());
            return null;
        }
        UserRegistry.User proto = getEmptyUser();
        proto.email = email;
        proto.name = name;
        proto.org = org;
        proto.setPassword(UserRegistry.makePassword(proto.email));
        proto.userlevel = level.getSTLevel();
        proto.locales = normLocales;

        User u = newUser(null, proto);
        return u;
    }

    public static JSONObject getLevelMenuJson(User me) throws JSONException {
        final VoteResolver.Level myLevel = VoteResolver.Level.fromSTLevel(me.userlevel);
        final Organization myOrganization = me.getOrganization();
        JSONObject levels = new JSONObject();
        for (VoteResolver.Level v : VoteResolver.Level.values()) {
            if (v == VoteResolver.Level.anonymous) {
                continue;
            }
            int number = v.getSTLevel(); // like 999
            JSONObject jo = new JSONObject();
            jo.put("name", v.name()); // like "locked"
            jo.put("string", UserRegistry.levelToStr(number)); // like "999: (LOCKED)"
            jo.put("canCreateOrSetLevelTo", myLevel.canCreateOrSetLevelTo(v));
            jo.put("isManagerFor", myLevel.isManagerFor(myOrganization, v, myOrganization));
            levels.put(String.valueOf(number), jo);
        }
        return levels;
    }
}
