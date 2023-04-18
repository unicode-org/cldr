package org.unicode.cldr.web;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RecentActivity {
    public static void getJson(
            SurveyJSONWrapper r, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String user = request.getParameter("user");
        if (user == null) {
            response.sendRedirect("survey");
            return;
        }
        r.put("newVersion", SurveyMain.getNewVersion());
        r.put("votesAfterDate", SurveyMain.getVotesAfterDate());
        r.put("user", user);
    }
}
