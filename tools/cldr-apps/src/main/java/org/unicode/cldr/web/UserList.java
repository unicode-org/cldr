package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.UserRegistry.InfoType;
import org.unicode.cldr.web.UserRegistry.User;

public class UserList {
    private static final Logger logger = SurveyLog.forClass(UserList.class);
    private static final boolean DEBUG = true;

    private static final String LIST_ACTION_SETLEVEL = "set_userlevel_";
    private static final String LIST_ACTION_NONE = "-";
    private static final String LIST_ACTION_SHOW_PASSWORD = "showpassword_";
    private static final String LIST_ACTION_SEND_PASSWORD = "sendpassword_";
    private static final String LIST_ACTION_SETLOCALES = "set_locales_";
    private static final String LIST_ACTION_DELETE0 = "delete0_";
    private static final String LIST_ACTION_DELETE1 = "delete_";
    private static final String LIST_JUST = "justu";
    private static final String LIST_MAILUSER = "mailthem";
    private static final String LIST_MAILUSER_WHAT = "mailthem_t";
    private static final String LIST_MAILUSER_CONFIRM = "mailthem_c";
    private static final String LIST_MAILUSER_CONFIRM_CODE = "confirm";

    private static final String PREF_SHOWLOCKED = "p_showlocked";
    private static final String PREF_JUSTORG = "p_justorg";

    private final boolean isValid;
    private final HttpServletRequest request;
    private final User me;
    private final SurveyMain sm;
    private final UserRegistry reg;
    private final WebContext ctx;

    /**
     * Is the info only about me (the current user)? true for My Account; false for List Users or
     * zoomed on other user
     */
    private final boolean isJustMe;

    /** email address of the single user (me or zoomed); null for list of users */
    private String just;

    private String justOrg;
    private String org;

    private final boolean canShowLocked;
    private final boolean showLocked;

    private EmailInfo emailInfo = null;

    public UserList(
            HttpServletRequest request,
            HttpServletResponse response,
            CookieSession mySession,
            SurveyMain sm)
            throws IOException {
        this.request = request;
        this.me = mySession.user;
        this.sm = sm;
        this.reg = sm.reg;

        // Note: we might not need ctx, could use request/response directly instead?
        ctx = new WebContext(request, response);
        ctx.session = mySession;
        ctx.sm = sm;
        request.setAttribute(WebContext.CLDR_WEBCONTEXT, ctx); // is this needed?
        justOrg = getParam(PREF_JUSTORG);
        if (justOrg != null && justOrg.isEmpty()) {
            justOrg = null;
        }
        just = getParam(LIST_JUST);
        if (just != null && just.isEmpty()) {
            just = null;
        }
        isJustMe = me.email.equals(just);
        org = me.org;
        if (UserRegistry.userIsAdmin(me)) {
            if (justOrg != null && !justOrg.equals("all")) {
                org = justOrg;
            } else {
                org = null; // all
            }
        }
        canShowLocked = UserRegistry.userIsExactlyManager(me) || UserRegistry.userIsTC(me);
        showLocked = canShowLocked && ctx.prefBool(PREF_SHOWLOCKED);
        isValid = isJustMe || UserRegistry.userCanListUsers(me);
    }

    public void getJson(SurveyJSONWrapper r) throws JSONException, SurveyException, IOException {
        if (!isValid) {
            r.put("err", "You do not have permission to list users.");
            return;
        }
        emailInfo = new EmailInfo(r);
        final String forOrg = UserRegistry.userIsAdmin(me) ? justOrg : me.org;
        final JSONObject userPerms = getUserPerms();
        r.put("what", SurveyAjax.WHAT_USER_LIST);
        r.put("org", forOrg);
        r.put("userPerms", userPerms);
        r.put("canShowLocked", canShowLocked);
        listUsers(r);
    }

    private JSONObject getUserPerms() throws JSONException {
        JSONObject userPerms = new JSONObject();
        final boolean canCreateUsers = UserRegistry.userCanCreateUsers(me);
        final boolean canModifyUsers = UserRegistry.userCanModifyUsers(me);
        userPerms.put("canCreateUsers", canCreateUsers);
        userPerms.put("canModifyUsers", canModifyUsers);
        if (canCreateUsers) {
            userPerms.put("levels", UserRegistry.getLevelMenuJson(me));
        }
        return userPerms;
    }

    private void listUsers(SurveyJSONWrapper r) throws JSONException {
        Connection conn = null;
        PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        try {
            conn = sm.dbUtils.getAConnection();
            synchronized (reg) {
                ps = reg.list(org, conn); // org = null to list all
                rs = ps.executeQuery();
                if (rs == null) {
                    return;
                }
                if (org == null) {
                    org = "ALL"; // after reg.list(org, conn)
                }
                JSONArray shownUsers = new JSONArray();
                while (rs.next()) {
                    handleOneRow(rs, shownUsers);
                }
                r.put("shownUsers", shownUsers);
                if (!isJustMe
                        && UserRegistry.userCanModifyUsers(me)
                        && UserRegistry.userCanEmailUsers(me)) {
                    int n = shownUsers.length();
                    if (n > 0) {
                        emailInfo.putStatus(n);
                    }
                }
                r.put(
                        "canSetInterestLocales",
                        isJustMe && UserRegistry.userCanSetInterestLocales(me));
                r.put("canGetEmailList", (!isJustMe) && UserRegistry.userCanGetEmailList(me));
            }
        } catch (SQLException se) {
            logger.log(
                    java.util.logging.Level.WARNING,
                    "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se),
                    se);
            r.put("exception", DBUtils.unchainSqlException(se));
        } finally {
            DBUtils.close(conn, ps, rs);
        }
    }

    /**
     * Handle one row of the database users table; skip if it shouldn't be shown
     *
     * @param rs the ResultSet for one db row
     * @param shownUsers the destination JSONArray
     * @throws SQLException
     * @throws JSONException
     */
    private void handleOneRow(ResultSet rs, JSONArray shownUsers)
            throws SQLException, JSONException {
        int id = rs.getInt(1);
        int level = rs.getInt(2);
        if (level == UserRegistry.ANONYMOUS) {
            return;
        }
        if (!showLocked
                && level >= UserRegistry.LOCKED
                && just == null /* if only one user, show regardless of lock state. */) {
            return;
        }
        UserSettings u = new UserSettings(id);
        if (just != null && !just.equals(u.user.email)) {
            return;
        }
        handleOneUser(shownUsers, u);
    }

    /**
     * Add information about the user to the json, and possibly handle actions specified in the
     * request related to the user
     *
     * <p>The combination of these two concerns -- add info and handle action -- is a legacy of
     * precursors of this code that didn't use json or ajax; probably the concerns should be
     * separated
     *
     * @param shownUsers the destination JSONArray
     * @param u the UserSettings of the user in question
     * @throws JSONException
     */
    private void handleOneUser(JSONArray shownUsers, UserSettings u) throws JSONException {
        String action = getParam(u.tag);
        if (emailInfo.areSendingMail && (u.user.userlevel < UserRegistry.LOCKED)) {
            MailSender.getInstance()
                    .queue(me.id, u.user.id, emailInfo.mailSubj, emailInfo.mailBody);
            action = "emailQueued";
            u.ua.put(action, "(queued)");
        }
        if (me.isAdminFor(u.user)) {
            handleActions(action, u);
        }
        putShownUser(shownUsers, u);
    }

    private void handleActions(String action, UserSettings u) {
        if (getParam(LIST_ACTION_SETLOCALES + u.tag).length() > 0) {
            setLocales(ctx.session, u);
        } else if (action == null || action.length() == 0 || action.equals(LIST_ACTION_NONE)) {
            return;
        } else if (action.startsWith(LIST_ACTION_SETLEVEL)) {
            setLevel(action, u, just);
        } else if (action.equals(LIST_ACTION_SHOW_PASSWORD)) {
            showPassword(u);
        } else if (action.equals(LIST_ACTION_SEND_PASSWORD)) {
            sendPassword(u);
        } else if (action.equals(LIST_ACTION_DELETE0)) {
            u.ua.put(
                    action,
                    "Ensure that 'confirm delete' is chosen at right and click Do Action to delete");
        } else if ((UserRegistry.userCanDeleteUser(me, u.user.id, u.user.userlevel))
                && (action.equals(LIST_ACTION_DELETE1))) {
            String s = reg.delete(ctx, u.user.id, u.user.email);
            s += "<strong style='font-color: red'>Deleting...</strong><br>";
            u.ua.put(action, s);
        } else if ((UserRegistry.userCanModifyUser(me, u.user.id, u.user.userlevel))
                && (action.equals(LIST_ACTION_SETLOCALES))) {
            changeLocales(action, u);
        } else if (UserRegistry.userCanModifyUser(me, u.user.id, u.user.userlevel)) {
            changeOther(action, u);
        } else if (u.user.id == me.id) {
            u.ua.put(action, "<i>You can't change that setting on your own account.</i>");
        } else {
            u.ua.put(action, "<i>No changes can be made to this user.</i>");
        }
    }

    private void setLocales(CookieSession session, UserSettings u) {
        String newLocales = getParam(LIST_ACTION_SETLOCALES + u.tag);

        String s = reg.setLocales(session, u.user, newLocales, false);
        u.user.locales = newLocales; // MODIFY
        if (u.session != null) {
            s +=
                    "<br/><i>Logging out user session "
                            + u.session.id
                            + " and deleting all unsaved changes</i>";
            u.session.remove();
        }
        UserRegistry.User newThem = reg.getInfo(u.user.id);
        if (newThem != null) {
            u.user.locales = newThem.locales; // update
        }
        u.ua.put(LIST_ACTION_SETLOCALES + u.tag, s);
    }

    private void setLevel(String action, UserSettings u, String just) {
        // check an explicit list. Don't allow random levels to be set.
        for (final VoteResolver.Level l : VoteResolver.Level.values()) { // like 999
            final int level = l.getSTLevel();
            if (action.equals(LIST_ACTION_SETLEVEL + level)) {
                String s = "";
                if ((just == null) && (level <= UserRegistry.TC)) {
                    s += "<b>Must be zoomed in on a user to promote them to TC</b>";
                } else if (!reg.canSetUserLevel(me, u.user, level)) {
                    s += "[Permission Denied]";
                } else {
                    u.user.userlevel = level;
                    s +=
                            "Set user level to "
                                    + UserRegistry.levelToStr(level)
                                    + ": "
                                    + reg.setUserLevel(me, u.user, level);
                    if (u.session != null) {
                        s += "<br/><i>Logging out user session " + u.session.id + "</i>";
                        u.session.remove();
                    }
                }
                u.ua.put(action, s);
                break;
            }
        }
    }

    private void showPassword(UserSettings u) {
        String pass = reg.getPassword(ctx, u.user.id);
        if (pass != null) {
            u.ua.put(LIST_ACTION_SHOW_PASSWORD, pass);
        }
    }

    private void sendPassword(UserSettings u) {
        String pass = reg.getPassword(ctx, u.user.id);
        if (pass != null && u.user.userlevel < UserRegistry.LOCKED) {
            u.ua.put(LIST_ACTION_SEND_PASSWORD, pass);
            sm.notifyUser(ctx, u.user.email, pass);
        }
    }

    private void changeLocales(String action, UserSettings u) {
        if (u.user.locales == null) {
            u.user.locales = "";
        }
        String s =
                "<label>Locales: (space separated) <input id='"
                        + LIST_ACTION_SETLOCALES
                        + u.tag
                        + "' name='"
                        + LIST_ACTION_SETLOCALES
                        + u.tag
                        + "' value='"
                        + u.user.locales
                        + "'></label>"
                        + "<button onclick=\"{document.getElementById('"
                        + LIST_ACTION_SETLOCALES
                        + u.tag
                        + "').value='*'; return false;}\" >All Locales</button>";
        u.ua.put(action, s);
    }

    private void changeOther(String action, UserSettings u) {
        UserRegistry.InfoType type = UserRegistry.InfoType.fromAction(action);
        if (UserRegistry.userIsAdmin(me) && type == UserRegistry.InfoType.INFO_PASSWORD) {
            changePassword(type, action, u);
        } else if (type != null) {
            changeUserInfo(type, action, u);
        }
    }

    private void changeUserInfo(InfoType type, String action, UserSettings u) {
        String what = type.toString();

        String s0 = getParam("string0" + what);
        String s1 = getParam("string1" + what);
        if (type == InfoType.INFO_ORG) {
            s1 = s0; /* ignore */
        }
        if (s0.equals(s1) && s0.length() > 0) {
            String s = "<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>";
            s +=
                    "<div class='fnotebox'>"
                            + reg.updateInfo(ctx, u.user.id, u.user.email, type, s0)
                            + "</div>";
            u.user = reg.getInfo(u.user.id);
            u.ua.put(action, s);
        } else {
            String s = "<h4>Change " + what + "</h4>";
            if (s0.length() > 0) {
                s += "<p class='ferrbox'>Both fields must match.</p>";
            }
            if (type == InfoType.INFO_ORG) {
                s += "<select name='string0" + what + "'>]n";
                s += "<option value='' >Choose...</option>\n";
                for (String o : UserRegistry.getOrgList()) {
                    s += "<option value='" + o + "' ";
                    if (o.equals(u.user.org)) {
                        s += " selected='selected' ";
                    }
                    s += ">" + o + "</option>";
                }
                s += "</select>";
            } else {
                s +=
                        "<label><b>New "
                                + what
                                + ":</b><input name='string0"
                                + what
                                + "' value='"
                                + s0
                                + "'></label><br />\n";
                s +=
                        "<label><b>New "
                                + what
                                + ":</b><input name='string1"
                                + what
                                + "'> (confirm)</label>\n";
            }
            u.ua.put(action, s);
        }
    }

    private void changePassword(UserRegistry.InfoType type, String action, UserSettings u) {
        String what = "password";
        String s0 = getParam("string0" + what);
        String s1 = getParam("string1" + what);
        if (s0.equals(s1) && s0.length() > 0) {
            String s = "<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>";
            s +=
                    "<div class='fnotebox'>"
                            + reg.updateInfo(ctx, u.user.id, u.user.email, type, s0)
                            + "</div>";
            u.ua.put(action, s);
        } else {
            String s = "<h4>Change " + what + "</h4>";
            if (s0.length() > 0) {
                s += "<p class='ferrbox'>Both fields must match.</p>";
            }
            s +=
                    "<p role='alert' style='font-size: 1.5em;'><em>PASSWORDS MAY BE VISIBLE AS PLAIN TEXT.";
            s += " USE OF A RANDOM PASSWORD (as suggested) IS STRONGLY RECOMMENDED.</em></p>";
            s +=
                    "<label><b>New "
                            + what
                            + ":</b><input type='password' name='string0"
                            + what
                            + "' value='"
                            + s0
                            + "'></label><br>";
            s +=
                    "<label><b>New "
                            + what
                            + ":</b><input type='password' name='string1"
                            + what
                            + "'> (confirm)</label>";
            s += "<br /><br />";
            s += "Suggested random password: <tt>" + UserRegistry.makePassword() + "</tt> )";
            u.ua.put(action, s);
        }
    }

    private void putShownUser(JSONArray shownUsers, UserSettings u) throws JSONException {
        User user = u.user;
        String active =
                (u.session == null)
                        ? ""
                        : SurveyMain.timeDiff(u.session.getLastBrowserCallMillisSinceEpoch());
        String seen =
                (user.last_connect == null) ? "" : SurveyMain.timeDiff(user.last_connect.getTime());
        boolean havePermToChange = me.isAdminFor(user);
        boolean userCanDeleteUser = UserRegistry.userCanDeleteUser(me, user.id, user.userlevel);
        VoteResolver.Level level = VoteResolver.Level.fromSTLevel(user.userlevel);
        shownUsers.put(
                new JSONObject()
                        .put("actions", u.ua)
                        .put("active", active)
                        .put("email", user.email)
                        .put("emailHash", user.getEmailHash())
                        .put("havePermToChange", havePermToChange)
                        .put("id", user.id)
                        .put("intlocs", user.intlocs)
                        .put("lastlogin", user.last_connect)
                        .put("locales", normalizeLocales(user.locales))
                        .put("name", user.name)
                        .put("org", user.org)
                        .put("seen", seen)
                        .put("userCanDeleteUser", userCanDeleteUser)
                        .put("userlevel", user.userlevel)
                        .put("userlevelName", level)
                        .put("votecount", level.getVotes(user.getOrganization()))
                        .put("voteCountMenu", level.getVoteCountMenu(user.getOrganization())));
    }

    /**
     * Normalize to be non-null and to use only spaces to separate multiple locales
     *
     * <p>The users table in the db is inconsistent in how lists of locales are formatted; some have
     * spaces, some have commas, some have commas and spaces. The front end expects them to be
     * separated only by single space characters.
     *
     * @param locales a string representing a set of locales
     * @return the normalized string
     */
    private static String normalizeLocales(String locales) {
        if (locales == null) {
            return "";
        } else {
            return locales.replaceAll("[,\\s]+", " ");
        }
    }

    /** Account settings for a particular user */
    private class UserSettings {
        UserRegistry.User user;
        String tag;
        CookieSession session;
        UserActions ua = new UserActions();

        public UserSettings(int id) {
            user = reg.getInfo(id);
            tag = id + "_" + user.email;
            session = CookieSession.retrieveUserWithoutTouch(user.email);
        }
    }

    private static class UserActions implements JSONString {
        HashMap<String, String> map = null;

        @Override
        public String toJSONString() throws JSONException {
            JSONObject j = new JSONObject();
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    j.put(key, value);
                }
            }
            return j.toString();
        }

        public void put(String key, String value) {
            if (key == null || key.isEmpty()) {
                System.out.println("Warning: invalid input to UserActions.put; value = " + value);
                return;
            }
            if (map == null) {
                map = new HashMap<>();
            }
            if (DEBUG) {
                if (map.containsKey(key)) {
                    System.out.println(
                            "Warning: UserActions.put changed value for "
                                    + key
                                    + " from "
                                    + map.get(key)
                                    + " to "
                                    + value);
                }
            }
            map.put(key, value);
        }
    }

    /**
     * Return true if the parameter is present in the request
     *
     * @param name the parameter name
     * @return true if the parameter is present
     *     <p>same as ctx.hasField
     */
    private boolean hasParam(String name) {
        return (request.getParameter(name) != null);
    }

    /**
     * Return a request parameter's value (if any), else ""
     *
     * @param name the parameter name
     * @return the parameter value, or else ""
     *     <p>often but not always the same as request.getParameter(name)
     *     <p>ctx.field never (?) returns null; it includes decodeFieldString...
     */
    private String getParam(String name) {
        return ctx.field(name);
    }

    /**
     * Information about the operation of composing and sending an email message to one or more
     * users in the list
     *
     * <p>Describes the whole operation, not specific to a particular recipient
     */
    private class EmailInfo {
        SurveyJSONWrapper r;
        String sendWhat;
        boolean areSendingMail = false;
        boolean didConfirmMail = false;
        // sending a dispute note?
        boolean areSendingDisp = (getParam(LIST_MAILUSER + "_d").length()) > 0;
        String mailBody = null;
        String mailSubj = null;

        public EmailInfo(SurveyJSONWrapper r) {
            this.r = r;
            sendWhat = getParam(LIST_MAILUSER_WHAT);
            if (sendWhat != null && !sendWhat.isEmpty()) {
                sendWhat = sendWhat.replaceAll("<br>", "\n");
            }
            if (UserRegistry.userCanEmailUsers(me)) {
                if (getParam(LIST_MAILUSER_CONFIRM).equals(LIST_MAILUSER_CONFIRM_CODE)) {
                    r.put("emailSendingMessage", true);
                    didConfirmMail = true;
                    mailBody =
                            "SurveyTool Message ---\n"
                                    + sendWhat
                                    + "\n--------\n\nSurvey Tool: "
                                    + CLDRConfig.getInstance().absoluteUrls().base()
                                    + "\n\n";
                    mailSubj = "CLDR SurveyTool message from " + me.name;
                    if (!areSendingDisp) {
                        areSendingMail = true; // we are ready to go ahead and mail
                    }
                } else if (hasParam(LIST_MAILUSER_CONFIRM)) {
                    r.put("emailMismatchWarning", true);
                }

                if (!areSendingMail && !areSendingDisp && hasParam(LIST_MAILUSER)) {
                    // hide the user list temporarily.
                    r.put("hideUserList", true);
                }
            }
        }

        public void putStatus(int n) {
            r.put("emailUserCount", n);
            if (getParam(LIST_MAILUSER).length() == 0) {
                r.put("emailStatus", "start");
            } else {
                boolean mismatch = false;
                if (sendWhat.length() > 0) {
                    if (!didConfirmMail) {
                        if (!getParam(LIST_MAILUSER_CONFIRM).equals(LIST_MAILUSER_CONFIRM_CODE)
                                && (getParam(LIST_MAILUSER_CONFIRM).length() > 0)) {
                            mismatch = true;
                        }
                    }
                }
                r.put("emailStatus", "continue");
                r.put("emailDidConfirm", didConfirmMail);
                r.put("emailSendingDisp", areSendingDisp);
                r.put("emailSendWhat", sendWhat);
                r.put("emailConfirmationMismatch", mismatch);
            }
        }
    }
}
