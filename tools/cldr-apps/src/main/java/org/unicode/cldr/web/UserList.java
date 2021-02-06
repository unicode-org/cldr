package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.UserRegistry.InfoType;
import org.unicode.cldr.web.UserRegistry.User;

public class UserList {

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

    private static final String GET_ORGS = "get_orgs";

    public void getJson(SurveyJSONWrapper r, HttpServletRequest request,
            HttpServletResponse response, CookieSession mySession, SurveyMain sm) throws JSONException, SurveyException, IOException {
        if (!mySession.user.isAdminForOrg(mySession.user.org)) {
            r.put("err", "You do not have permission to list users.");
            return;
        }
        final String justOrg = request.getParameter(PREF_JUSTORG);
        final String forOrg = (UserRegistry.userIsAdmin(mySession.user)) ? justOrg : mySession.user.org;
        final JSONObject userPerms = getUserPerms(mySession.user);
        r.put("what", SurveyAjax.WHAT_USER_LIST);
        r.put("org", forOrg);
        r.put("userPerms", userPerms);
        if ("true".equals(request.getParameter(GET_ORGS))) {
            sm.reg.setOrgList();
            r.put("orgList", UserRegistry.getOrgList());
        }

        // DEBUGGING print all the parameters (query string and post body):
        Map<String, String[]> m = request.getParameterMap();
        Set<Entry<String, String[]>> s = m.entrySet();
        Iterator<Entry<String, String[]>> it = s.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String[]> entry = it.next();
            String key = entry.getKey();
            String[] value = entry.getValue();
            System.out.println("Key: " + key);
            System.out.println("Value: " + value[0].toString());
            System.out.println("-------------------");
        }

        // TODO: figure out what is really needed to get ctx for doList;
        // some of this may be pointless
        // CLDRConfigImpl.setUrls(request);
        final WebContext ctx = new WebContext(request, response);
        request.setAttribute(WebContext.CLDR_WEBCONTEXT, ctx);
        ctx.session = mySession;
        ctx.sm = sm;
        // Call doList to get json.shownUsers, json.preset_fromint, etc.
        doList(r, ctx, sm, justOrg);
    }

    private static JSONObject getUserPerms(User me) throws JSONException {
        JSONObject userPerms = new JSONObject();
        final boolean canCreateUsers = UserRegistry.userCanCreateUsers(me);
        final boolean canModifyUsers = UserRegistry.userCanModifyUsers(me);
        userPerms.put("canCreateUsers", canCreateUsers);
        userPerms.put("canModifyUsers", canModifyUsers);
        if (canCreateUsers) {
            final VoteResolver.Level myLevel = VoteResolver.Level.fromSTLevel(me.userlevel);
            final Organization myOrganization = me.getOrganization();
            JSONObject levels = new JSONObject();
            for (VoteResolver.Level v : VoteResolver.Level.values()) {
                int number = v.getSTLevel(); // like 999
                JSONObject jo = new JSONObject();
                jo.put("name", v.name()); // like "locked"
                jo.put("string", UserRegistry.levelToStr(number)); // like "999: (LOCKED)"
                jo.put("canCreateOrSetLevelTo", myLevel.canCreateOrSetLevelTo(v));
                jo.put("isManagerFor", myLevel.isManagerFor(myOrganization, v, myOrganization));
                levels.put(String.valueOf(number), jo);
            }
            userPerms.put("levels", levels);
        }
        return userPerms;
    }

    private void doList(SurveyJSONWrapper r, WebContext ctx, SurveyMain sm, String justOrg) throws JSONException {
        boolean justme = false; // "my account" mode
        String just = ctx.field(LIST_JUST);
        if (just != null) {
            if (just.isEmpty()) {
                just = null;
            } else if (ctx.session.user.email.equals(just)) {
                justme = true;
            }
        }
        String org = ctx.session.user.org;
        if (UserRegistry.userIsAdmin(ctx.session.user)) {
            if (justOrg != null && !justOrg.equals("all")) {
                org = justOrg;
            } else {
                org = null; // all
            }
        }
        String sendWhat = ctx.field(LIST_MAILUSER_WHAT);
        boolean areSendingMail = false;
        boolean didConfirmMail = false;
        boolean showLocked = ctx.prefBool(PREF_SHOWLOCKED);
        // sending a dispute note?
        boolean areSendingDisp = (ctx.field(LIST_MAILUSER + "_d").length()) > 0;
        String mailBody = null;
        String mailSubj = null;
        boolean hideUserList = false;
        if (UserRegistry.userCanEmailUsers(ctx.session.user)) {
            if (ctx.field(LIST_MAILUSER_CONFIRM).equals(LIST_MAILUSER_CONFIRM_CODE)) {
                /// ctx.println("<h1>sending mail to users...</h4>");
                didConfirmMail = true;
                mailBody = "SurveyTool Message ---\n" + sendWhat
                    + "\n--------\n\nSurvey Tool: http://st.unicode.org" + ctx.base() + "\n\n";
                mailSubj = "CLDR SurveyTool message from " + ctx.session.user.name;
                if (!areSendingDisp) {
                    areSendingMail = true; // we are ready to go ahead and mail..
                }
            } else if (ctx.hasField(LIST_MAILUSER_CONFIRM)) {
                r.put("email_mismatch", true);
            }

            if (!areSendingMail && !areSendingDisp && ctx.hasField(LIST_MAILUSER)) {
                hideUserList = true; // hide the user list temporarily.
            }
        }
        Connection conn = null;
        PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        UserRegistry reg = sm.reg;
        int n = 0;
        try {
            conn = sm.dbUtils.getDBConnection();
            synchronized (reg) {
                ps = reg.list(org, conn);
                rs = ps.executeQuery();
                if (rs == null) {
                    return;
                }
                if (org == null) {
                    org = "ALL";
                }
                // Preset box
                boolean preFormed = false;

                if (hideUserList) {
                    String warnHash = "userlist";
                    /***
                    ctx.println("<div id='h_" + warnHash + "'><a href='javascript:show(\"" + warnHash + "\")'>"
                        + "<b>+</b> Click here to show the user list...</a></div>");
                    ctx.println("<!-- <noscript>Warning: </noscript> -->" + "<div style='display: none' id='" + warnHash + "'>");
                    ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + "(<b>- hide userlist</b>)</a><br>");
                     ***/
                }
                int preset_fromint = ctx.fieldInt("preset_from", -1);
                String preset_do = ctx.field("preset_do");
                if (preset_do.equals(LIST_ACTION_NONE)) {
                    preset_do = "nothing";
                }
                r.put("preset_fromint", preset_fromint);
                r.put("preset_do", preset_do);
                String oldOrg = null;
                int locked = 0;
                JSONArray shownUsers = new JSONArray();
                while (rs.next()) {
                    // SELECT id,userlevel,name,email,org,locales,intlocs,lastlogin
                    int theirId = rs.getInt(1);
                    int theirLevel = rs.getInt(2);
                    if (theirLevel == UserRegistry.ANONYMOUS) {
                        continue; // never list or email anonymous users
                    }
                    if (!showLocked
                        && theirLevel >= UserRegistry.LOCKED
                        && just == null /* if only one user, show regardless of lock state. */) {
                        locked++;
                        continue;
                    }
                    String theirName = DBUtils.getStringUTF8(rs, 3);
                    String theirEmail = rs.getString(4);
                    String theirOrg = rs.getString(5);
                    String theirLocales = rs.getString(6);
                    String theirIntLocs = rs.getString(7);
                    java.sql.Timestamp theirLast = rs.getTimestamp(8);
                    UserRegistry.User theirInfo = reg.getInfo(theirId);
                    boolean havePermToChange = ctx.session.user.isAdminFor(theirInfo);

                    // theirTag: prevents stale data (such as delete of user 3 if the rows change)
                    String theirTag = theirId + "_" + theirEmail;
                    String action = ctx.field(theirTag);
                    CookieSession theUser = CookieSession.retrieveUserWithoutTouch(theirEmail);
                    if (just != null && !just.equals(theirEmail)) {
                        continue;
                    }
                    n++;
                    UserActions ua = new UserActions();
                    if ((just == null) && (!justme) && (!theirOrg.equals(oldOrg))) {
                        oldOrg = theirOrg;
                    }
                    if (areSendingMail && (theirLevel < UserRegistry.LOCKED)) {
                        // TODO: queued
                        /// ctx.print("<td class='framecell'>");
                        MailSender.getInstance().queue(ctx.userId(), theirId, mailSubj, mailBody);
                        /// ctx.println("(queued)</td>");
                    }
                    if (havePermToChange) {
                        if (ctx.field(LIST_ACTION_SETLOCALES + theirTag).length() > 0) {
                            theirLocales = setLocales(ctx, theirTag, theUser, theirId, theirEmail, reg, ua);
                        } else if ((action != null) && (action.length() > 0) && (!action.equals(LIST_ACTION_NONE))) {
                            if (action.startsWith(LIST_ACTION_SETLEVEL)) {
                                theirLevel = setLevel(action, ctx, theUser, theirLevel, theirId, theirEmail, just, reg, ua);
                                theirInfo.userlevel = theirLevel;
                            }
                            else if (action.equals(LIST_ACTION_SHOW_PASSWORD)) {
                                showPassword(ctx, theirId, reg, ua);
                            } else if (action.equals(LIST_ACTION_SEND_PASSWORD)) {
                                sendPassword(ctx, theirId, theirLevel, theirEmail, sm, reg, ua);
                            } else if (action.equals(LIST_ACTION_DELETE0)) {
                                ua.put(action, "Ensure that 'confirm delete' is chosen at right and click Do Action to delete");
                            } else if ((UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel))
                                && (action.equals(LIST_ACTION_DELETE1))) {
                                String s = reg.delete(ctx, theirId, theirEmail);
                                s += "<strong style='font-color: red'>Deleting...</strong><br>";
                                ua.put(action, s);
                            } else if ((UserRegistry.userCanModifyUser(ctx.session.user, theirId, theirLevel))
                                && (action.equals(LIST_ACTION_SETLOCALES))) {
                                if (theirLocales == null) {
                                    theirLocales = "";
                                }
                                String s = "<label>Locales: (space separated) <input id='"
                                    + LIST_ACTION_SETLOCALES + theirTag + "' name='"
                                    + LIST_ACTION_SETLOCALES + theirTag
                                    + "' value='" + theirLocales + "'></label>"
                                    + "<button onclick=\"{document.getElementById('"
                                    + LIST_ACTION_SETLOCALES + theirTag
                                    + "').value='*'; return false;}\" >All Locales</button>";
                                ua.put(action, s);
                            } else if (UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel)) {
                                // change of other stuff.
                                UserRegistry.InfoType type = UserRegistry.InfoType.fromAction(action);
                                // e.g., action = CHANGE_INFO_PASSWORD
                                if (UserRegistry.userIsAdmin(ctx.session.user) && type == UserRegistry.InfoType.INFO_PASSWORD) {
                                    String what = "password";

                                    String s0 = ctx.field("string0" + what);
                                    String s1 = ctx.field("string1" + what);
                                    if (s0.equals(s1) && s0.length() > 0) {
                                        String s = "<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>";
                                        s += "<div class='fnotebox'>" + reg.updateInfo(ctx, theirId, theirEmail, type, s0) + "</div>";
                                        s += "<i>click Change again to see changes</i>";
                                        ua.put(action, s);
                                        action = ""; // don't popup the menu again. (???)
                                    } else {
                                        /// ctx.println("<h4>Change " + what + "</h4>");
                                        if (s0.length() > 0) {
                                            /// ctx.println("<p class='ferrbox'>Both fields must match.</p>");
                                        }
                                        /***
                                        ctx.println(
                                            "<p role='alert' style='font-size: 1.5em;'><em>PASSWORDS MAY BE VISIBLE AS PLAIN TEXT. USE OF A RANDOM PASSWORD (as suggested) IS STRONGLY RECOMMENDED.</em></p>");
                                        ctx.println("<label><b>New " + what + ":</b><input type='password' name='string0" + what
                                            + "' value='" + s0 + "'></label><br>");
                                        ctx.println("<label><b>New " + what + ":</b><input type='password' name='string1" + what
                                            + "'> (confirm)</label>");

                                        ctx.println("<br><br>");
                                        ctx.println("(Suggested random password: <tt>" + UserRegistry.makePassword(theirEmail)
                                            + "</tt> )");
                                            ***/
                                    }
                                } else if (type != null) {
                                    String what = type.toString();

                                    String s0 = ctx.field("string0" + what);
                                    String s1 = ctx.field("string1" + what);
                                    if (type == InfoType.INFO_ORG)
                                        s1 = s0; /* ignore */
                                    if (s0.equals(s1) && s0.length() > 0) {
                                        String s = "<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>";
                                        s += "<div class='fnotebox'>" + reg.updateInfo(ctx, theirId, theirEmail, type, s0) + "</div>";
                                        s += "<i>click Change again to see changes</i>";
                                        ua.put(action, s);
                                        action = ""; // don't popup the menu again. (?)
                                    } else {
                                        String s = "<h4>Change " + what + "</h4>";
                                        if (s0.length() > 0) {
                                            s += "<p class='ferrbox'>Both fields must match.</p>";
                                        }
                                        // TODO: menu (on front end, not back end!!!)
                                        /***
                                        if (type == InfoType.INFO_ORG) {
                                            ctx.println("<select name='string0" + what + "'>");
                                            ctx.println("<option value='' >Choose...</option>");
                                            for (String o : UserRegistry.getOrgList()) {
                                                ctx.print("<option value='" + o + "' ");
                                                if (o.equals(theirOrg)) {
                                                    ctx.print(" selected='selected' ");
                                                }
                                                ctx.println(">" + o + "</option>");
                                            }
                                            ctx.println("</select>");
                                        } else {
                                            ctx.println("<label><b>New " + what + ":</b><input name='string0" + what
                                                + "' value='" + s0 + "'></label><br>");
                                            ctx.println("<label><b>New " + what + ":</b><input name='string1" + what
                                                + "'> (confirm)</label>");
                                        }
                                        ***/
                                    }
                                }
                            } else if (theirId == ctx.session.user.id) {
                                String s = "<i>You can't change that setting on your own account.</i>";
                                ua.put(action, s);
                            } else {
                                String s = "<i>No changes can be made to this user.</i>";
                                ua.put(action, s);
                            }
                            // ctx.println("Change to " + action);
                        }
                    }
                    /***
                    ctx.println("</td>");

                    // org, level
                    ctx.println("    <td>" + theirOrg + "<br>" + "&nbsp; <span style='font-size: 80%' align='right'>"
                        + UserRegistry.levelToStr(ctx, theirLevel).replaceAll(" ", "&nbsp;") + "</span></td>");

                    ctx.println("    <td valign='top'><font size='-1'>#" + theirId + " </font> <a name='u_" + theirEmail + "'>"
                        + theirName + "</a>");
                    ctx.println("    <a href='mailto:" + theirEmail + "'>" + theirEmail + "</a>");
                    ctx.print("</td><td>");
                    ***/
                    if (havePermToChange) {
                        // Was something requested?
                        {
                            if (just != null) {
                                /***
                                if (havePermToChange) {
                                    ctx.println("   <option ");
                                    ctx.println(" value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>");
                                }
                                if (UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel)) {
                                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                                    if ((action != null) && action.equals(LIST_ACTION_DELETE0)) {
                                        ctx.println("   <option value='" + LIST_ACTION_DELETE1
                                            + "' SELECTED>Confirm delete</option>");
                                    } else {
                                        ctx.println("   <option ");
                                        if ((preset_fromint == theirLevel) && preset_do.equals(LIST_ACTION_DELETE0)) {
                                            // ctx.println(" SELECTED ");
                                        }
                                        ctx.println(" value='" + LIST_ACTION_DELETE0 + "'>Delete user..</option>");
                                    }
                                }
                                if (just != null) { // only do these in 'zoomin'
                                    // view.
                                    ctx.println("   <option disabled>" + LIST_ACTION_NONE + "</option>");

                                    InfoType current = InfoType.fromAction(action);
                                    for (InfoType info : InfoType.values()) {
                                        if (info == InfoType.INFO_ORG && !(ctx.session.user.userlevel == UserRegistry.ADMIN)) {
                                            continue;
                                        }
                                        ctx.print(" <option ");
                                        if (info == current) {
                                            ctx.print(" SELECTED ");
                                        }
                                        ctx.println(" value='" + info.toAction() + "'>Change " + info.toString() + "...</option>");
                                    }
                                }
                                ***/
                            }
                        } // end menu
                    }
                    /***
                    if (ctx.session.user.isAdminFor(reg.getInfo(theirId))) {
                        ctx.println("<br><a href='" + ctx.context("upload.jsp?s=" + ctx.session.id + "&email=" + theirEmail)
                            + "'>Upload XML...</a>");
                    }
                    ctx.println("<br><a class='recentActivity' href='" + ctx.context("myvotes.jsp?user=" + theirId) + "'>User Activity</a>");
                    ctx.println("</td>");

                    if (theirLevel <= UserRegistry.MANAGER) {
                        ctx.println(" <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
                    } else {
                        ctx.println(" <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                    }

                    // are they logged in?
                    if ((theUser != null) && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                        ctx.println("<td>");
                        ctx.println("<b>active: " + SurveyMain.timeDiff(theUser.getLastBrowserCallMillisSinceEpoch()) + " ago</b>");
                        if (UserRegistry.userIsAdmin(ctx.session.user)) {
                            ctx.print("<br/>");
                            printLiveUserMenu(ctx, theUser);
                        }
                        ctx.println("</td>");
                    } else if (theirLast != null) {
                        ctx.println("<td>");
                        ctx.println("<b>seen: " + SurveyMain.timeDiff(theirLast.getTime()) + " ago</b>");
                        ctx.print("<br/><font size='-2'>");
                        ctx.print(theirLast.toString());
                        ctx.println("</font></td>");
                    }

                    ctx.println("  </tr>");
                    ***/

                    putShownUser(shownUsers, ctx.session.user, theirInfo, theUser, theirLocales, theirIntLocs, theirLast, ua);
                }
                r.put("shownUsers", shownUsers);
                if (!justme) {
                    /***
                    ctx.println("<div style='font-size: 70%'>Number of users shown: " + n + "</div><br>");

                    if (n == 0 && just != null && !just.isEmpty()) {
                        UserRegistry.User u = reg.get(just);
                        if (u == null) {
                            ctx.println("<h3 class='ferrbox'>" + ctx.iconHtml("stop", "Not Found Error") + " User '" + just
                                + "' does not exist.</h3>");
                        } else {
                            ctx.println("<h3 class='ferrbox'>" + ctx.iconHtml("stop", "Not Found Error") + " User '" + just
                                + "' from organization " + u.org + " is not visible to you. Ask an administrator.</h3>");
                        }
                    }
                    ***/
                    if ((UserRegistry.userIsExactlyManager(ctx.session.user) || UserRegistry.userIsTC(ctx.session.user))
                        && locked > 0) {
                        /*** TODO
                        sm.showTogglePref(subCtx, PREF_SHOWLOCKED, "Show " + locked + " locked users:");
                        ***/
                    }
                }
                if (!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    if ((n > 0) && UserRegistry.userCanEmailUsers(ctx.session.user)) {
                        /*
                         * send a mass email to users
                         */
                        /***
                        if (ctx.field(LIST_MAILUSER).length() == 0) {
                            ctx.println("<label><input type='checkbox' value='y' name='" + LIST_MAILUSER
                                + "'>Check this box to compose a message to these " + n
                                + " users (excluding LOCKED users).</label>");
                        } else {
                            ctx.println("<p><div class='pager'>");
                            ctx.println("<h4>Mailing " + n + " users</h4>");
                            if (didConfirmMail) {
                                if (areSendingDisp) {
                                    throw new InternalError("Not implemented - see DisputePageManager");
                                } else {
                                    ctx.println("<b>Mail sent.</b><br>");
                                }
                            } else { // dont' allow resend option
                                ctx.println("<input type='hidden' name='" + LIST_MAILUSER + "' value='y'>");
                            }
                            ctx.println("From: <b>(depends on recipient organization)</b><br>");
                            if (sendWhat.length() > 0) {
                                ctx.println("<div class='odashbox'>"
                                    + TransliteratorUtilities.toHTML.transliterate(sendWhat).replaceAll("\n", "<br>")
                                    + "</div>");
                                if (!didConfirmMail) {
                                    ctx.println("<input type='hidden' name='" + LIST_MAILUSER_WHAT + "' value='"
                                        + sendWhat.replaceAll("&", "&amp;").replaceAll("'", "&quot;") + "'>");
                                    if (!ctx.field(LIST_MAILUSER_CONFIRM).equals(LIST_MAILUSER_CONFIRM_CODE)
                                        && (ctx.field(LIST_MAILUSER_CONFIRM).length() > 0)) {
                                        ctx.println("<strong>" + ctx.iconHtml("stop", "confirmation did not match")
                                            + "That confirmation didn't match. Try again.</strong><br>");
                                    }
                                    ctx.println("To confirm sending, type the confirmation code <tt class='codebox'>"
                                        + LIST_MAILUSER_CONFIRM_CODE
                                        + "</tt> in this box : <input name='" + LIST_MAILUSER_CONFIRM + "'>");
                                }
                            } else {
                                ctx.println("<textarea NAME='" + LIST_MAILUSER_WHAT
                                    + "' id='body' ROWS='15' COLS='85' style='width:100%'></textarea>");
                            }
                            ctx.println("</div>");
                        }
                        ***/
                    }
                }
                // #level $name $email $org

                // more 'My Account' stuff
                if (justme) {
                    /// ctx.println("<hr>");
                    // Is the 'interest locales' list relevant?
                    if (ctx.session.user.userlevel <= UserRegistry.EXPERT) {
                        boolean intlocs_change = (ctx.field("intlocs_change").length() > 0);

                        /// ctx.println("<h4>Notify me about these locale groups (just the language names, no underscores or dashes):</h4>");

                        if (intlocs_change) {
                            if (ctx.field("intlocs_change").equals("t")) {
                                String newIntLocs = ctx.field("intlocs");

                                String msg = reg.setLocales(ctx, ctx.session.user.id, ctx.session.user.email, newIntLocs, true);

                                if (msg != null) {
                                    /// ctx.println(msg + "<br>");
                                }
                                UserRegistry.User newMe = reg.getInfo(ctx.session.user.id);
                                if (newMe != null) {
                                    ctx.session.user.intlocs = newMe.intlocs; // update
                                }
                            }
                            /***
                            ctx.println("<input type='hidden' name='intlocs_change' value='t'>");
                            ctx.println("<label>Locales: <input name='intlocs' ");
                            if (ctx.session.user.intlocs != null) {
                                ctx.println("value='" + ctx.session.user.intlocs.trim() + "' ");
                            }
                            ctx.println("</input></label>");
                            if (ctx.session.user.intlocs == null) {
                                ctx.println(
                                    "<br><i>List languages only, separated by spaces.  Example: <tt class='codebox'>en fr zh</tt>. leave blank for 'all locales'.</i>");
                            }
                            ***/
                        }
                        /***
                        ctx.println("<ul><tt class='codebox'>" + UserRegistry.prettyPrintLocale(ctx.session.user.intlocs)
                            + "</tt>");
                        if (!intlocs_change) {
                            ctx.print("<a href='" + ctx.url() + ctx.urlConnector() + "do=listu&" + LIST_JUST + "="
                                + URLEncoder.encode(ctx.session.user.email) + "&intlocs_change=b' >[Change this]</a>");
                        }
                        ctx.println("</ul>");
                        ***/
                    } // end intlocs
                    /// ctx.println("<br>");
                }
                if (justme || UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    /***
                    ctx.println("<br>");
                    ctx.println("<input type='submit' name='doBtn' value='Do Action'>");
                    ctx.println("</form>");

                    if (!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                        WebContext subsubCtx = new WebContext(ctx);
                        subsubCtx.addQuery("s", ctx.session.id);
                        if (org != null) {
                            subsubCtx.addQuery("org", org);
                        }
                        subsubCtx.addQuery("do", "list");
                        subsubCtx.println("<hr><form method='POST' action='" + subsubCtx.context("DataExport.jsp") + "'>");
                        subsubCtx.printUrlAsHiddenFields();
                        subsubCtx.print("<input type='submit' class='csvDownload' value='Download .csv (including LOCKED)'>");
                        subsubCtx.println("</form>");
                    }
                    ***/
                }
            } /* end synchronized(reg) */
        } catch (SQLException se) {
            SurveyLog.logger.log(java.util.logging.Level.WARNING,
                "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se), se);
            /// ctx.println("<i>Failure: " + DBUtils.unchainSqlException(se) + "</i><br>");
        } finally {
            DBUtils.close(conn, ps, rs);
        }
    }

    private void showPassword(WebContext ctx, int theirId, UserRegistry reg, UserActions ua) {
        String pass = reg.getPassword(ctx, theirId);
        if (pass != null) {
            ua.put(LIST_ACTION_SHOW_PASSWORD, pass);
        }
    }

    private void sendPassword(WebContext ctx, int theirId, int theirLevel, String theirEmail, SurveyMain sm, UserRegistry reg, UserActions ua) {
        String pass = reg.getPassword(ctx, theirId);
        if (pass != null && theirLevel < UserRegistry.LOCKED) {
            ua.put (LIST_ACTION_SEND_PASSWORD, pass);
            sm.notifyUser(ctx, theirEmail, pass);
        }
    }

    private String setLocales(WebContext ctx, String theirTag, CookieSession theUser, int theirId, String theirEmail, UserRegistry reg, UserActions ua) {
        String newLocales = ctx.field(LIST_ACTION_SETLOCALES + theirTag);
        String s = reg.setLocales(ctx, theirId, theirEmail, newLocales);
        String theirLocales = newLocales; // MODIFY
        if (theUser != null) {
            s += "<br/><i>Logging out user session " + theUser.id
                + " and deleting all unsaved changes</i>";
            theUser.remove();
        }
        UserRegistry.User newThem = reg.getInfo(theirId);
        if (newThem != null) {
            theirLocales = newThem.locales; // update
        }
        ua.put(LIST_ACTION_SETLOCALES + theirTag,  s);
        return theirLocales;
    }

    private int setLevel(String action, WebContext ctx, CookieSession theUser, int theirLevel, int theirId, String theirEmail,
            String just, UserRegistry reg, UserActions ua) {
        // check an explicit list. Don't allow random levels to be set.
        for (int i = 0; i < UserRegistry.ALL_LEVELS.length; i++) {
            int level = UserRegistry.ALL_LEVELS[i];
            if (action.equals(LIST_ACTION_SETLEVEL + level)) {
                String s = "";
                if ((just == null) && (level <= UserRegistry.TC)) {
                    s += "<b>Must be zoomed in on a user to promote them to TC</b>";
                } else {
                    theirLevel = level;
                    s += "Set user level to "
                        + UserRegistry.levelToStr(level) + ": "
                        + reg.setUserLevel(ctx, theirId, theirEmail, level);
                    if (theUser != null) {
                        s += "<br/><i>Logging out user session " + theUser.id + "</i>";
                        theUser.remove();
                    }
                }
                ua.put(action, s);
                break;
            }
        }
        return theirLevel;
    }

    private static void putShownUser(JSONArray shownUsers, User me, User them,
            CookieSession theirSession, String theirLocales, String theirIntLocs,
            Timestamp theirLast, UserActions ua) throws JSONException {
        String active = (theirSession == null) ? "" : SurveyMain.timeDiff(theirSession.getLastBrowserCallMillisSinceEpoch());
        String seen = (theirLast == null) ? "" : SurveyMain.timeDiff(theirLast.getTime());
        boolean havePermToChange = me.isAdminFor(them);
        boolean userCanDeleteUser = UserRegistry.userCanDeleteUser(me, them.id, them.userlevel);
        VoteResolver.Level level = VoteResolver.Level.fromSTLevel(them.userlevel);
        shownUsers.put(new JSONObject()
            .put("actions", ua)
            .put("active", active)
            .put("email", them.email)
            .put("emailHash", them.getEmailHash())
            .put("havePermToChange", havePermToChange)
            .put("id", them.id)
            .put("intlocs", theirIntLocs)
            .put("lastlogin", theirLast)
            .put("locales", normalizeLocales(theirLocales))
            .put("name", them.name)
            .put("org", them.org)
            .put("seen", seen)
            .put("userCanDeleteUser", userCanDeleteUser)
            .put("userlevel", them.userlevel)
            .put("userlevelName", level)
            .put("votecount", level.getVotes())
            .put("voteCountMenu", level.getVoteCountMenu())
        );
    }

    /**
     * Normalize to be non-null and to use only spaces to separate multiple locales
     *
     * The users table in the db is inconsistent in how lists of locales are formatted;
     * some have spaces, some have commas, some have commas and spaces. The front end
     * expects them to be separated only by single space characters.
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

    public class UserActions implements JSONString {
        HashMap<String, String> map = null;

        @Override
        public String toJSONString() throws JSONException {
            JSONObject j = new JSONObject();
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    j.put(key,  value);
                }
            }
            return j.toString();
        }

        public void put(String key, String value) {
            if (map == null) {
                map = new HashMap<>();
            }
            map.put(key, value);
        }
    }
}
