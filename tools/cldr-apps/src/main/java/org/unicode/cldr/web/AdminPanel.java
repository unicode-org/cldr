package org.unicode.cldr.web;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.VoteResolver;

public class AdminPanel {
    public void getJson(SurveyJSONWrapper r, HttpServletRequest request, HttpServletResponse response, SurveyMain sm) throws JSONException, IOException {
        /*
         * Assume caller has already confirmed UserRegistry.userIsAdmin
         */
        String action = request.getParameter("do");
        if (action == null || action.isEmpty()) {
            System.out.println("Warning: AdminPanel.getJson called without do parameter!");
            return;
        }
        if (action.equals("users")) {
            listUsers(r);
        } else if (action.equals("unlink")) {
            unlinkUser(r, request);
        } else if (action.equals("threads")) {
            showThreads(r);
        } else if (action.equals("exceptions")) {
            showExceptions(r, request);
        } else if (action.equals("settings")) {
            showSettings(r);
        } else if (action.equals("settings_set")) {
            setSettings(r, request);
        } else if (action.equals("create_login")) {
            createAndLogin(r, request, response, sm);
        } else {
            r.put("err", "Unknown action: " + action);
        }
    }

    private void listUsers(SurveyJSONWrapper r) throws JSONException {
        JSONObject users = new JSONObject();
        for (CookieSession cs : CookieSession.getAllSet()) {
            JSONObject sess = new JSONObject();
            if (cs.user != null) {
                sess.put("user", SurveyJSONWrapper.wrap(cs.user));
            }
            sess.put("id", cs.id);
            sess.put("ip", cs.ip);
            sess.put("lastBrowserCallMillisSinceEpoch", SurveyMain.timeDiff(cs.getLastBrowserCallMillisSinceEpoch()));
            sess.put("lastActionMillisSinceEpoch", SurveyMain.timeDiff(cs.getLastActionMillisSinceEpoch()));
            sess.put("millisTillKick", cs.millisTillKick());
            users.put(cs.id, sess);
        }
        r.put("users", users);
    }

    private void unlinkUser(SurveyJSONWrapper r, HttpServletRequest request) throws JSONException {
        String s = request.getParameter("s");
        CookieSession cs = CookieSession.retrieveWithoutTouch(s);
        if (cs != null) {
            JSONObject sess = new JSONObject();
            if (cs.user != null) {
                sess.put("user", SurveyJSONWrapper.wrap(cs.user));
            }
            sess.put("id", cs.id);
            sess.put("ip", cs.ip);
            sess.put("lastBrowserCallMillisSinceEpoch", SurveyMain.timeDiff(cs.getLastBrowserCallMillisSinceEpoch()));
            sess.put("lastActionMillisSinceEpoch", SurveyMain.timeDiff(cs.getLastActionMillisSinceEpoch()));
            sess.put("millisTillKick", cs.millisTillKick());
            r.put("kick", s);
            r.put("removing", sess);
            cs.remove();
        } else {
            r.put("kick", s);
            r.put("removing", null);
        }
    }

    private void showThreads(SurveyJSONWrapper r) throws JSONException {
        JSONObject threads = new JSONObject();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long deadlockedThreads[] = threadBean.findDeadlockedThreads();
        if (deadlockedThreads != null) {
            JSONArray dead = new JSONArray();
            ThreadInfo deadThreadInfo[] = threadBean.getThreadInfo(
                deadlockedThreads, true, true);
            for (ThreadInfo deadThread : deadThreadInfo) {
                dead.put(new JSONObject()
                    .put("name", deadThread.getThreadName())
                    .put("id", deadThread.getThreadId())
                    .put("text", deadThread.toString()));
            }
            threads.put("dead", dead);
        }
        Map<Thread, StackTraceElement[]> s = Thread.getAllStackTraces();
        JSONObject threadList = new JSONObject();
        for (Map.Entry<Thread, StackTraceElement[]> e : s.entrySet()) {
            Thread t = e.getKey();
            JSONObject thread = new JSONObject()
                .put("state", t.getState())
                .put("name", t.getName())
                .put("stack", new JSONArray(e.getValue()));
            threadList.put(Long.toString(t.getId()), thread);
        }
        threads.put("all", threadList);
        r.put("threads", threads);
    }

    private void showExceptions(SurveyJSONWrapper r, HttpServletRequest request) throws JSONException, IOException {
        JSONObject exceptions = new JSONObject();
        ChunkyReader cr = SurveyLog.getChunkyReader();
        exceptions.put("lastTime", cr.getLastTime());
        ChunkyReader.Entry e = null;
        if (request.getParameter("before") != null) {
            Long before = Long.parseLong(request.getParameter("before"));
            e = cr.getEntryBelow(before);
        } else {
            e = cr.getLastEntry();
        }
        if (e != null) {
            exceptions.put("entry", e);
        }
        r.put("exceptions", exceptions);
    }

    private void showSettings(SurveyJSONWrapper r) throws JSONException {
        CLDRConfigImpl cci = (CLDRConfigImpl) (CLDRConfig.getInstance());
        JSONObject all = new JSONObject().put("all", cci.toJSONObject());
        r.put("settings", all);
    }

    private void setSettings(SurveyJSONWrapper r, HttpServletRequest request) throws JSONException {
        JSONObject settings = new JSONObject();
        try {
            String setting = request.getParameter("setting");
            StringBuilder sb = new StringBuilder();
            java.io.Reader reader = request.getReader();
            int ch;
            while ((ch = reader.read()) > -1) {
                sb.append((char) ch);
            }
            CLDRConfig cci = (CLDRConfig.getInstance());
            cci.setProperty(setting, sb.toString());
            settings.put("ok", true);
            settings.put(setting, cci.getProperty(setting));
        } catch (Throwable t) {
            SurveyLog.logException(t, "Tring to set setting ");
            settings.put("err", t.toString());
        }
        r.put("settings_set", settings);
    }

    /**
     * Create a new temporary user, and login as that user
     *
     * @param r
     * @param request
     * @param response
     * @param sm
     *
     * Earlier version was in createAndLogin.jsp
     * @throws JSONException
     */
    private void createAndLogin(SurveyJSONWrapper r, HttpServletRequest request, HttpServletResponse response, SurveyMain sm) throws JSONException {
        if (SurveyMain.isSetup == false) {
            r.put("isSetup", false);
            return;
        }
        // zap any current login
        Cookie c0 = WebContext.getCookie(request, SurveyMain.QUERY_EMAIL);
        if (c0 != null) {
            c0.setValue("");
            c0.setMaxAge(0);
            response.addCookie(c0);
        }
        Cookie c1 = WebContext.getCookie(request, SurveyMain.QUERY_PASSWORD);
        if (c1 != null) {
            c1.setValue("");
            c1.setMaxAge(0);
            response.addCookie(c1);
        }
        String orgs[] = UserRegistry.getOrgList();
        String myorg = orgs[(int) Math.rint(Math.random() * (orgs.length - 1))];
        JSONObject levels = new JSONObject();
        for (final VoteResolver.Level l : VoteResolver.Level.values()) { // like 999
            final int lev = l.getSTLevel();
            if (lev != UserRegistry.ADMIN) {
                JSONObject jo = new JSONObject();
                jo.put("name", UserRegistry.levelToStr(lev)); // like "locked"
                jo.put("string", UserRegistry.levelToStr(lev)); // like "999: (LOCKED)"
                levels.put(String.valueOf(lev), jo);
            }
        }
        r.put("name", randomName());
        r.put("orgs", orgs);
        r.put("defaultOrg", myorg);
        r.put("levels", levels);
        r.put("defaultLevel", UserRegistry.TC);
    }

    static final String allNames[] = {
        // http://en.wikipedia.org/wiki/List_of_most_popular_given_names (Greenland)
        "Ivaana", "Pipaluk", "Nivi", "Paninnguaq", "Ivalu", "Naasunnguaq", "Julie", "Ane", "Isabella", "Kimmernaq",
        "Malik", "Aputsiaq", "Minik", "Hans", "Inunnguaq", "Kristian", "Nuka", "Salik", "Peter", "Inuk",
    };

    private String randomName() {
        // generate random name
        StringBuilder genname = new StringBuilder();

        genname.append(choose(allNames));
        genname.append(' ');
        genname.append((char) ('A' + new Random().nextInt(26)));
        genname.append('.');
        genname.append(' ');
        genname.append(choose("Vetter", "Linguist", "User", "Typer", "Tester", "Specialist",
            "Person", "Account", "Login", "CLDR"));
        return genname.toString();
    }

    private String choose(String... option) {
        return option[new Random().nextInt(option.length)];
    }
}
