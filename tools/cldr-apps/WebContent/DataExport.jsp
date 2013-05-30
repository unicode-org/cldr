<%@page import="org.unicode.cldr.util.SimpleXMLSource"
        %><%@page import="org.unicode.cldr.util.CLDRFile"
        %><%@page import="org.unicode.cldr.util.CLDRLocale"
        %><%@ page language="java" contentType="text/csv; charset=UTF-8" import="org.unicode.cldr.web.*" pageEncoding="UTF-8"
        %><%
    String s = request.getParameter("s");
    String action = request.getParameter("do");
    String org = request.getParameter("org");
    String user = request.getParameter("user");
    String loc = request.getParameter("_");
    CookieSession sess = null;
    if (s != null) {
        sess = CookieSession.retrieveWithoutTouch(s);
    }
    SurveyMain sm = CookieSession.sm;
    if (action == null || action.isEmpty() || s == null || sess == null) {
        response.sendRedirect(request.getContextPath());
        return;
    }

    if (action.equals("list") && UserRegistry.userIsVetter(sess.user)) {
        if ("ALL".equals(org)) {
            org = null;
        }
        if (!UserRegistry.userCreateOtherOrgs(sess.user)) {
            org = sess.user.org;
        }
        response.setHeader("content-disposition", "attachment;  filename=\"SurveyTool_" + ((org == null) ? "ALL" : org) + "_list.csv" + "\"");

        java.sql.Connection conn = null;
        java.sql.ResultSet rs = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            synchronized (sm.reg) {
                rs = sm.reg.list(org, conn);

                DBUtils.writeCsv(rs, out);

            }/*
             * end synchronized(reg)
             */
        } catch (java.sql.SQLException se) {
            SurveyLog.logException(se, "listing users in DataExport.jsp");
            response.sendError(500, "SQL error: " + se.getMessage());
            return;
        } finally {
            DBUtils.close(rs, conn);
        }
    } else if (action.equals("myxml")) {
        int u = -1;
        System.err.println("user=" + user);
        if (user != null && !user.isEmpty()) {
            try {
                u = Integer.parseInt(user);
            } catch (Throwable t) {
                SurveyLog.logException(t, "Parsing user " + user);
            } 
        }
        
        if(u==-1) {
            response.sendError(500, "unknown/invalid user code");
            return;
        }
        
        CLDRLocale l = CLDRLocale.getInstance(loc);
        
        response.setContentType("application/xml");
        response.setHeader("content-disposition", "attachment;  filename=\""+loc+"_" + u + ".xml" + "\"");

        java.sql.Connection conn = null;
        java.sql.ResultSet rs = null;
        java.sql.PreparedStatement stmt = null;
        String sql = "(none)";
        String votesAfter = SurveyMain.getSQLVotesAfter();
        try {
            conn = DBUtils.getInstance().getDBConnection();
            synchronized (sm.reg) {
                String q1 = "select cldr_votevalue.xpath, cldr_votevalue.value from cldr_votevalue where cldr_votevalue.submitter = ? and cldr_votevalue.value is not NULL  and cldr_votevalue.locale=? and cldr_votevalue.last_mod > " + votesAfter;
                stmt = DBUtils.prepareStatementWithArgs(conn, sql = (q1), u,l.getBaseName());
                rs = stmt.executeQuery();
                
                SimpleXMLSource sxs = new SimpleXMLSource(l.getBaseName());

                while(rs.next()) {
                    sxs.putValueAtPath(sess.sm.xpt.getById(rs.getInt(1)),DBUtils.getStringUTF8(rs, 2));
                }

                CLDRFile cf = new CLDRFile(sxs);

                cf.setInitialComment("SurveyTool votes for user " + u + " on " + new java.util.Date());
                
                
                cf.write(response.getWriter());
                
            }/*
             * end synchronized(reg)
             */
        } catch (java.sql.SQLException se) {
            SurveyLog.logException(se, "Dumping data in DataExport.jsp, query=" + sql);
            response.sendError(500, "SQL error: " + se.getMessage());
            return;
        } finally {
            DBUtils.close(stmt, rs, conn);
        }
        
    } else if (action.equals("mydata")) {

        int u = -1;
        System.err.println("user=" + user);
        if (user != null && !user.isEmpty()) {
            try {
                u = Integer.parseInt(user);
            } catch (Throwable t) {
                SurveyLog.logException(t, "Parsing user " + user);
            }
        }
        
        if(u==-1) return;

        /*
         * if(u==null) { response.sendError(500, "unknown/invalid
         * user"); return; }
         */

        response.setHeader("content-disposition", "attachment;  filename=\"SurveyTool_userdata" + u + ".csv" + "\"");

        java.sql.Connection conn = null;
        java.sql.ResultSet rs = null;
        java.sql.PreparedStatement stmt = null;
        String sql = "(none)";
        try {
            conn = DBUtils.getInstance().getDBConnection();
            synchronized (sm.reg) {
                String q1 = "select cldr_votevalue.locale,cldr_xpaths.xpath, cldr_votevalue.value, cldr_votevalue.last_mod  from cldr_xpaths,cldr_votevalue,cldr_users  where ";
                String q2 = "cldr_xpaths.id=cldr_votevalue.xpath and cldr_users.id=cldr_votevalue.submitter and cldr_votevalue.value is not NULL ";
                stmt = DBUtils.prepareStatementWithArgs(conn, sql = (q1 + q2 + " and cldr_votevalue.submitter=?"), u);
                rs = stmt.executeQuery();
                DBUtils.writeCsv(rs, out);

            }/*
             * end synchronized(reg)
             */
        } catch (java.sql.SQLException se) {
            SurveyLog.logException(se, "Dumping data in DataExport.jsp, query=" + sql);
            response.sendError(500, "SQL error: " + se.getMessage());
            return;
        } finally {
            DBUtils.close(stmt, rs, conn);
        }
    } else {
        response.sendError(500, "unknown/invalid action code");
        return;
    }

%>