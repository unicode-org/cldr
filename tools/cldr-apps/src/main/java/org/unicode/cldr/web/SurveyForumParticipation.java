package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.util.JSONException;

public class SurveyForumParticipation {

    public static final class SurveyForumParticipationHelper {
        static SurveyForumParticipationHelper INSTANCE = new SurveyForumParticipationHelper();
        public Throwable err;

        private SurveyForumParticipationHelper() {
            // set up our stored procedure.
            String sqlName = "cldr-forum-participation.sql";

            try {
                DBUtils.execSql(sqlName);
            } catch (IOException | SQLException e) {
                err = e;
            }
        }
    }

    private String org;
    private Set<String> orgLocales;

    public SurveyForumParticipation(String org) {
        this.org = org;
        orgLocales = StandardCodes.make().getLocaleCoverageLocales(org);
    }

    public void getJson(SurveyJSONWrapper r) throws JSONException, SQLException, IOException {
        Throwable t = SurveyForumParticipationHelper.INSTANCE.err;
        if (t != null) {
            if (t instanceof SQLException) {
                throw (SQLException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            }
        }
        r.put("org", org);
        r.put("orgLocales", orgLocales);

        r.put(
                "rows",
                DBUtils.queryToJSON(
                        "CALL cldr_forum_participation(?, ?)", org, SurveyMain.getNewVersion()));
    }
}
