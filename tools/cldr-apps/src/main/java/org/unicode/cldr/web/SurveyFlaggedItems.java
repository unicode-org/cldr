package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.SQLException;
import org.unicode.cldr.web.util.JSONException;
import org.unicode.cldr.web.util.JSONObject;

public class SurveyFlaggedItems {
    private boolean userIsTC;

    public SurveyFlaggedItems(boolean userIsTC) {
        this.userIsTC = userIsTC;
    }

    public void getJson(SurveyJSONWrapper r) throws JSONException, SQLException, IOException {
        JSONObject results =
                DBUtils.queryToCachedJSON(
                        SurveyAjax.WHAT_FLAGGED,
                        5 * 1000,
                        "SELECT * FROM "
                                + DBUtils.Table.VOTE_FLAGGED
                                + " ORDER BY locale ASC, last_mod DESC");
        r.put("flagged", results);
        if (userIsTC) {
            String detailQuery =
                    "SELECT flag.locale AS locale, flag.last_mod AS last_mod,"
                            + " user.id AS user, user.org AS org, user.email AS email, xpath.xpath AS xpath"
                            + " FROM "
                            + DBUtils.Table.VOTE_FLAGGED
                            + " AS flag, cldr_xpaths AS xpath, cldr_users AS user WHERE xpath.id = flag.xpath"
                            + " AND user.id = flag.submitter ORDER BY last_mod DESC";
            r.put("details", DBUtils.queryToJSON(detailQuery));
        }
    }
}
