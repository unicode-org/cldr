package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.unicode.cldr.util.XMLSource;

public class SurveyBulkClosePosts {

    private SurveyMain sm;

    private boolean execute;

    /**
     * Construct a SurveyBulkClosePosts
     *
     * @param sm the SurveyMain
     * @param execute if true, actually close the posts; else report count and provide button
     */
    public SurveyBulkClosePosts(SurveyMain sm, boolean execute) {
        this.sm = sm;
        this.execute = execute;
        reportOrExecute();
    }

    /**
     * The number of threads to close; after doExecute, the number of threads actually closed
     *
     * A "thread" is an initial post (not a reply), plus any posts that are replies to it
     */
    private int threadCount = 0;

    /**
     * The number of posts (including replies) actually closed; only set after doExecute (for efficiency)
     */
    private int postCount = 0;

    private ArrayList<Integer> rootIdList = new ArrayList<>();

    private Connection conn = null;

    private ResultSet rs = null;

    private PreparedStatement ps = null;

    private String errCode = null;

    private void reportOrExecute() {
        try {
            conn = DBUtils.getInstance().getDBConnection();
            prepareOpenRequestsDetailQuery();
            rs = ps.executeQuery();
            while (rs.next()) {
                updateList(rs);
            }
            if (execute && rootIdList.size() > 0) {
                doExecute();
            }
            threadCount = rootIdList.size();
        } catch (SQLException e) {
            SurveyLog.logException(e, "getJson");
            errCode = e.toString();
        } finally {
            DBUtils.close(rs, ps, conn);
            rs = null;
            ps = null;
            conn = null;
        }
    }

    public void getJson(SurveyJSONWrapper r) throws JSONException, SQLException {
        if (errCode != null) {
            r.put("status", "error");
            r.put("err", errCode);
        } else {
            r.put("status", execute ? "done" : "ready");
            r.put("threadCount", threadCount);
            if (execute) {
                r.put("postCount", postCount);
            }
        }
    }

    private void prepareOpenRequestsDetailQuery() throws SQLException {
        String sql = "SELECT id,loc,xpath,value"
            + " FROM " + DBUtils.Table.FORUM_POSTS.toString()
            + " WHERE is_open=TRUE"
            + " AND type=?";
        ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps.setInt(1, SurveyForum.PostType.REQUEST.toInt());
    }

    private void updateList(ResultSet rs) throws SQLException {
        Integer id = rs.getInt(1);
        String loc = rs.getString(2);
        Integer xpath = rs.getInt(3);
        String value = DBUtils.getStringUTF8(rs, 4);
        if (matchesWinning(loc, xpath, value)) {
            rootIdList.add(id);
        }
    }

    private boolean matchesWinning(String loc, Integer xpath, String value) {
        XMLSource diskData = sm.getDiskFactory().makeSource(loc).freeze();
        String xpathString = sm.xpt.getById(xpath);
        String curValue = diskData.getValueAtDPath(xpathString);
        return diskData.equalsOrInheritsCurrentValue(value, curValue, xpathString);
    }

    private void doExecute() {
        try {
            postCount = SurveyForum.closeThreads(conn, rootIdList);
            conn.commit(); // without commit here, posts are not closed
        } catch (SQLException e) {
            SurveyLog.logException(e, "doExecute");
            errCode = e.toString();
        }
    }
}
