package org.unicode.cldr.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.web.util.JSONException;
import org.unicode.cldr.web.util.JSONObject;

public class ErrorSubtypes {

    public static void getJson(SurveyJSONWrapper r, HttpServletRequest request)
            throws MalformedURLException, JSONException {
        final String recheck = request.getParameter("flush");
        if (recheck != null) {
            getRecheck(r, recheck);
            return;
        }
        r.put("COMMENT", SubtypeToURLMap.COMMENT);
        r.put("BEGIN_MARKER", SubtypeToURLMap.BEGIN_MARKER);
        r.put("END_MARKER", SubtypeToURLMap.END_MARKER);
        getMap(r);
    }

    private static void getMap(SurveyJSONWrapper r) throws MalformedURLException, JSONException {
        SubtypeToURLMap map = SubtypeToURLMap.getInstance();
        if (map == null) {
            return;
        }
        getUrlStatus(r, map);
        getUnhandledTypes(r, map);
    }

    private static void getUrlStatus(SurveyJSONWrapper r, SubtypeToURLMap map)
            throws JSONException, MalformedURLException {
        List<JSONObject> uList = new ArrayList<>();
        for (final String u : map.getUrls()) {
            Integer checkStatus = HttpStatusCache.check(new URL(u));
            JSONObject urlStatus = new JSONObject();
            urlStatus.put("url", u);
            urlStatus.put("status", checkStatus);
            if (HttpStatusCache.isGoodStatus(checkStatus)) {
                List<String> strings = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (final Subtype s : map.getSubtypesForUrl(u)) {
                    strings.add(s.toString());
                    names.add(s.name());
                }
                urlStatus.put("strings", strings);
                urlStatus.put("names", names);
            }
            uList.add(urlStatus);
        }
        r.put("urls", uList);
    }

    private static void getUnhandledTypes(SurveyJSONWrapper r, SubtypeToURLMap map)
            throws JSONException {
        if (!map.getUnhandledTypes().isEmpty()) {
            JSONObject unhandled = new JSONObject();
            List<String> strings = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (final Subtype sub : map.getUnhandledTypes()) {
                strings.add(sub.toString());
                names.add(sub.name());
            }
            unhandled.put("strings", strings);
            unhandled.put("names", names);
            r.put("unhandled", unhandled);
        }
    }

    private static void getRecheck(SurveyJSONWrapper r, String recheck)
            throws MalformedURLException {
        if (recheck.startsWith("MAP")) {
            try {
                // load directly to make sure there are no errors
                SubtypeToURLMap map = SubtypeToURLMap.reload();
                if (map == null) {
                    r.put("err", "FAILED. Check for errors.");
                } else {
                    // SubtypeToURLMap.setDefaultInstance(map);
                    r.put("status", "SUCCESS!");
                    SubtypeToURLMap.setInstance(map);
                }
            } catch (Throwable t) {
                r.put("err", "Reload FAILED");
                r.put("stack", t.toString());
                return; // do not auto refresh.
            }
        } else if (recheck.startsWith("http")) {
            r.put("status", "Flushing " + recheck + " from cache...");
            HttpStatusCache.flush(new URL(recheck));
        } else {
            r.put("status", "Flushing cache...");
            HttpStatusCache.flush(null);
        }
    }
}
