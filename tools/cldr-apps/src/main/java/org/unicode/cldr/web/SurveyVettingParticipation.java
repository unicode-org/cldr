package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TimeDiff;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.util.JSONArray;
import org.unicode.cldr.web.util.JSONException;
import org.unicode.cldr.web.util.JSONObject;

public class SurveyVettingParticipation {
    private final String org;
    private final SurveyMain sm;

    public SurveyVettingParticipation(String org, SurveyMain sm) {
        this.sm = sm;
        this.org = org;
    }

    public void getJson(SurveyJSONWrapper r)
            throws SQLException, JSONException, ExecutionException {
        Connection conn = null;
        PreparedStatement psParticipation = null;
        PreparedStatement psUsers = null;
        ResultSet rs, rsu;
        JSONArray userObj = new JSONArray();
        JSONArray participationObj = new JSONArray();
        try {
            conn = DBUtils.getInstance().getAConnection();
            psUsers = sm.reg.list(org, conn);
            if (org == null) {
                r.put("org", "*");
                psParticipation =
                        conn.prepareStatement(
                                "SELECT v.submitter, v.locale,"
                                        + " max(v.last_mod) AS last_mod"
                                        + " FROM "
                                        + DBUtils.Table.VOTE_VALUE
                                        + " AS v"
                                        + " GROUP BY v.locale, v.submitter",
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);

            } else {
                r.put("org", org);
                // same, but restrict by org
                psParticipation =
                        conn.prepareStatement(
                                "SELECT v.submitter, v.locale,"
                                        + " max(v.last_mod) AS last_mod"
                                        + " FROM "
                                        + DBUtils.Table.VOTE_VALUE
                                        + " AS v, cldr_users AS u"
                                        + " WHERE v.submitter = u.id"
                                        + " AND org = ?"
                                        + " GROUP BY v.locale, v.submitter",
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                psParticipation.setString(1, org);
            }

            // first, collect coverage
            rsu = psUsers.executeQuery();
            while (rsu.next()) {
                int theirId = rsu.getInt("id");
                final User theUser = sm.reg.getInfo(theirId);

                if (UserRegistry.userIsLocked(theUser)
                        || UserRegistry.userIsManagerOrStronger(theUser)
                        || !UserRegistry.userIsGuestOrStronger(theUser)) {
                    continue; // skip these
                }
                // Note: this json includes some data not currently (2025-07) used by the
                // client for vetting participation, such as user.emailHash and user.time.
                final JSONObject json = SurveyJSONWrapper.wrap(theUser);
                userObj.put(json);

                final LocaleSet intSet = theUser.getInterestLocales();
                if (intSet == null || intSet.isEmpty() || intSet.isAllLocales()) {
                    json.put("allLocales", true);
                } else {
                    JSONArray intArr = new JSONArray();
                    for (final CLDRLocale l : intSet.getSet()) {
                        intArr.put(l.getBaseName());
                    }
                    json.put("locales", intArr);
                }
            }

            rs = psParticipation.executeQuery();

            while (rs.next()) {
                String locale = rs.getString("v.locale");
                if (StandardCodes.ALL_LOCALES.equals(locale)) { // "*"
                    continue;
                }
                int theirId = rs.getInt("v.submitter");
                // daysAgo = how many days ago was the most recent vote by this submitter
                Date whenVoted = new Date(rs.getTimestamp("last_mod").getTime());
                long daysAgo = TimeDiff.daysSinceDate(whenVoted);
                participationObj.put(
                        new JSONObject()
                                .put("user", theirId)
                                .put("locale", locale)
                                .put("daysAgo", daysAgo));
            }
            rs.close();
        } finally {
            DBUtils.close(psUsers, psParticipation, conn);
        }
        r.put("users", userObj);
        r.put("participation", participationObj);
    }
}
