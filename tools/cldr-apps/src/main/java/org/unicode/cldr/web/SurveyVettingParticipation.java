package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.UserRegistry.User;

public class SurveyVettingParticipation {
    static final private int STREET_STLEVEL = VoteResolver.Level.street.getSTLevel();

    final private String org;
    final private String missingLocalesForOrg;
    final private SurveyMain sm;

    public SurveyVettingParticipation(String org, SurveyMain sm) {
        this.sm = sm;
        this.org = org;
        this.missingLocalesForOrg = org;
    }

    public void getJson(SurveyJSONWrapper r) throws SQLException, JSONException {
        final StandardCodes sc = StandardCodes.make();
        Set<CLDRLocale> allVettedLocales = new HashSet<>();
        Connection conn = null;
        PreparedStatement psParticipation = null;
        PreparedStatement psUsers = null;
        ResultSet rs = null, rsu = null;
        Map<CLDRLocale, User> localeToUser = new TreeMap<>();
        JSONArray userObj = new JSONArray();
        JSONArray participationObj = new JSONArray();
        try {
            conn = DBUtils.getInstance().getAConnection();
            psUsers = sm.reg.list(org, conn);
            if (org == null) {
                r.put("org", "*");
                psParticipation = conn.prepareStatement("SELECT v.submitter, count(v.submitter) as count, v.locale \n"
                    + "    FROM " + DBUtils.Table.VOTE_VALUE + " as v\n"
                    + " group by v.locale, v.submitter;",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            } else {
                r.put("org", org);
                // same, but restrict by org
                psParticipation = conn.prepareStatement("SELECT v.submitter, count(v.submitter) as count, v.locale \n"
                    + "    FROM " + DBUtils.Table.VOTE_VALUE + " as v, cldr_users as u\n"
                    + "    WHERE v.submitter = u.id\n"
                    + "     AND org = ?\n"
                    + " group by v.locale, v.submitter;",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                psParticipation.setString(1, org);
            }

            // first, collect coverage
            rsu = psUsers.executeQuery();
            while (rsu.next()) {
                int theirId = rsu.getInt("id");
                final User theUser = sm.reg.getInfo(theirId);

                if ((theUser.userlevel > STREET_STLEVEL) ||
                    UserRegistry.userIsLocked(theUser) // skip these
                ) {
                    continue;
                }
                final JSONObject json = SurveyJSONWrapper.wrap(theUser);
                userObj.put(json);

                final Set<CLDRLocale> intSet = theUser.getInterestLocales(true);
                if (intSet == null) {
                    json.put("allLocales", true);
                } else {
                    JSONArray intArr = new JSONArray();
                    for (final CLDRLocale l : intSet) {
                        localeToUser.put(l, theUser);
                        intArr.put(l.getBaseName());
                        allVettedLocales.add(l); // covered by a user
                    }
                    json.put("locales", intArr);
                }
            }

            rs = psParticipation.executeQuery();

            while (rs.next()) {
                int theirId = rs.getInt("v.submitter");
                int count = rs.getInt("count");
                String locale = rs.getString("v.locale");

                participationObj.put(new JSONObject()
                    .put("user", theirId)
                    .put("count", count)
                    .put("locale", locale));
            }
            rs.close();
        } finally {
            DBUtils.close(psUsers, psParticipation, conn);
        }

        r.put("users", userObj);
        r.put("participation", participationObj);

        Set<CLDRLocale> allLanguages = new TreeSet<>();

        for (String code : sc.getAvailableCodes("language")) {
            allLanguages.add(CLDRLocale.getInstance(code));
        }

        if (missingLocalesForOrg != null) {
            // calculate coverage of requested locales for this organization
            Set<CLDRLocale> languagesNotInCLDR = new TreeSet<>();
            Set<CLDRLocale> languagesMissing = new HashSet<>();
            r.put("missingLocalesForOrg", missingLocalesForOrg);
            for (Iterator<CLDRLocale> li = allLanguages.iterator(); li.hasNext();) {
                CLDRLocale lang = (li.next());
                String group = sc.getGroup(lang.getBaseName(), missingLocalesForOrg);
                if ((group != null) &&
                    (null == sm.getSupplementalDataInfo().getBaseFromDefaultContent(CLDRLocale.getInstance(group)))) {
                    if (!sm.isValidLocale(lang)) {
                        languagesNotInCLDR.add(lang);
                    } else if (!allVettedLocales.contains(lang)) {
                        languagesMissing.add(lang);
                    }
                }
            }
            r.put("languagesNotInCLDR", SurveyJSONWrapper.wrap(languagesNotInCLDR));
            r.put("languagesMissing", SurveyJSONWrapper.wrap(languagesMissing));
        }
    }
}
