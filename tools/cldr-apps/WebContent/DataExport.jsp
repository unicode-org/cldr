<%@ page language="java" contentType="text/csv; charset=UTF-8"
	import="org.unicode.cldr.web.*" pageEncoding="UTF-8"%><%String s = request.getParameter("s");
			String action = request.getParameter("do");
			String org = request.getParameter("org");
			CookieSession sess = null;
			if (s != null) {
				sess = CookieSession.retrieveWithoutTouch(s);
			}
			if (action == null || action.isEmpty() || s == null || sess == null
					|| !UserRegistry.userIsVetter(sess.user)) {
				response.sendRedirect(request.getContextPath());
				return;
			}
			SurveyMain sm = CookieSession.sm;

			if (action.equals("list")) {
				if ("ALL".equals(org)) {
					org = null;
				}
				if (!UserRegistry.userCreateOtherOrgs(sess.user)) {
					org = sess.user.org;
				}
				response.setHeader("content-disposition", "attachment;  filename=\"SurveyTool_"  + ((org==null)?"ALL":org) +   "_list.csv" +  "\"");   

				java.sql.Connection conn = null;
				java.sql.ResultSet rs = null;
				try {
					conn = DBUtils.getInstance().getDBConnection();
					synchronized (sm.reg) {
						rs = sm.reg.list(org, conn);
						
						DBUtils.writeCsv(rs,out);
						
					}/*end synchronized(reg)*/
				} catch (java.sql.SQLException se) {
					SurveyLog.logException(se, "listing users in DataExport.jsp");
					response.sendError(500, "SQL error: " + se.getMessage());
					return;
				} finally {
					DBUtils.close(rs,conn);
				}

			} else {
				response.sendError(500, "unknown action code");
				return;
			}
			
%>