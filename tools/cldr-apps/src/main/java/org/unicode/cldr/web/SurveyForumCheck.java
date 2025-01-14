package org.unicode.cldr.web;

import static org.unicode.cldr.web.SurveyForum.NO_PARENT;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import org.unicode.cldr.icu.dev.util.ElapsedTimer;

public class SurveyForumCheck {
    private final Logger logger;

    private final Connection conn;

    private final Map<Integer, TestPostInfo> map;

    private int errorCount = 0;

    public SurveyForumCheck(SurveyMain sm) {
        this.logger = SurveyLog.forClass(SurveyForumCheck.class);
        // In spite of setting Level.INFO here, the Appender may discard anything less than
        // Level.WARNING
        logger.setLevel(java.util.logging.Level.INFO);
        this.conn = sm.dbUtils.getAConnection();
        this.map = new HashMap<>();
    }

    public void run() {
        ElapsedTimer et = new ElapsedTimer();
        try {
            loadPosts();
            checkPosts();
            // Need logger.warning, not logger.info, or we may not see it in the console
            logger.warning(
                    "SurveyForumCheck: loaded "
                            + map.size()
                            + " posts; found "
                            + errorCount
                            + " error(s); time: "
                            + et);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPosts() throws SQLException {
        final String sql = "SELECT id,parent,root,forum FROM " + DBUtils.Table.FORUM_POSTS;
        PreparedStatement ps = DBUtils.prepareStatement(conn, "SurveyForumCheck", sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            int parent = rs.getInt(2);
            int root = rs.getInt(3);
            int forum = rs.getInt(4);
            map.put(id, new TestPostInfo(parent, root, forum));
        }
        rs.close();
    }

    private void checkPosts() {
        for (Map.Entry<Integer, TestPostInfo> entry : map.entrySet()) {
            final int id = entry.getKey();
            final TestPostInfo info = entry.getValue();
            if ((info.parent == NO_PARENT) != (info.root == NO_PARENT)) {
                fail(
                        id,
                        info,
                        "each post has parent = NO_PARENT if and only if root = NO_PARENT",
                        null);
            } else if (info.parent == id) {
                fail(id, info, "no post has id = parent", null);
            } else if (info.root == id) {
                fail(id, info, "no post has id = root", null);
            } else if (info.parent != NO_PARENT && !map.containsKey(info.parent)) {
                fail(
                        id,
                        info,
                        "if a post has parent = n, then a post with id = n must exist",
                        null);
            } else if (info.root != NO_PARENT && !map.containsKey(info.root)) {
                fail(id, info, "if a post has root = n, then a post with id = n must exist", null);
            } else {
                traverseToRoot(id, info);
            }
        }
    }

    private void traverseToRoot(int origId, final TestPostInfo origInfo) {
        TestPostInfo info = origInfo;
        int count = 0;
        while (info.parent != NO_PARENT) {
            int id = info.parent;
            info = map.get(id);
            if (info.forum != origInfo.forum) {
                fail(
                        origId,
                        origInfo,
                        "posts in same thread have same forum id",
                        "reached id " + id + "; forum " + info.forum);
                return;
            } else if (info.root != NO_PARENT && info.root != origInfo.root) {
                fail(
                        origId,
                        origInfo,
                        "posts in same thread have same root",
                        "reached id " + id + "; root " + info.root);
                return;
            } else if (info.root == NO_PARENT && id != origInfo.root) {
                fail(
                        origId,
                        origInfo,
                        "root post id matches descendants",
                        "reached id " + id + "; root " + info.root);
                return;
            } else if (++count > 1000) {
                fail(origId, origInfo, "no thread has more than 1000 generations (loop?)", null);
                return;
            }
        }
    }

    private void fail(int id, TestPostInfo info, String expectation, String details) {
        ++errorCount;
        logger.severe(
                "SurveyForumCheck: FAILED expectation that "
                        + expectation
                        + "; id = "
                        + id
                        + "; parent = "
                        + subst(info.parent)
                        + "; root = "
                        + subst(info.root)
                        + "; forum = "
                        + subst(info.forum)
                        + ((details == null) ? "" : " (" + details + ")"));
    }

    private String subst(int i) {
        return i == NO_PARENT ? "NO_PARENT" : ("" + i);
    }

    private static class TestPostInfo {
        final int parent, root, forum;

        TestPostInfo(int parent, int root, int forum) {
            this.parent = parent;
            this.root = root;
            this.forum = forum;
        }
    }
}
