<%@page import="com.ibm.icu.dev.test.util.ElapsedTimer"%><%@page
	import="org.unicode.cldr.web.*,java.util.Map,java.util.Set,java.util.Comparator,java.util.TreeMap,java.util.TreeSet"%><%@ page
	language="java" contentType="application/json; charset=UTF-8"
	import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.json.*"%><%--  Copyright (C) 2012 IBM and Others. All Rights Reserved 
	 --%><%@ page import="java.lang.management.*"%><%
	String vap = request.getParameter("vap");
	String action = request.getParameter("do");
	if (vap == null || action == null
			|| (SurveyMain.vap == null || SurveyMain.vap.isEmpty())
			|| vap.length() == 0 || (!SurveyMain.vap.equals(vap))) {
		response.sendError(500,
				"Your session has timed out or the SurveyTool has restarted.");
		return;
	}

	if (action.equals("users")) {

		JSONObject users = new JSONObject();
		for (CookieSession cs : CookieSession.getAllSet()) {
			JSONObject sess = new JSONObject();
			if (cs.user != null) {
				sess.put("user", SurveyAjax.JSONWriter.wrap(cs.user));
			}
			sess.put("id", cs.id);
			sess.put("ip", cs.ip);
			sess.put("last", SurveyMain.timeDiff(cs.last));
			//			sess.put("locales",new JSONArray().put(cs.getLocales().keys()));
			users.put(cs.id, sess);
		}
		new JSONWriter(out).object().key("users").value(users)
				.endObject();

	} else if (action.equals("threads")) {

		JSONObject threads = new JSONObject();

		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

		long deadlockedThreads[] = threadBean.findDeadlockedThreads();
		// detect deadlocks
		if (deadlockedThreads != null) {
			JSONArray dead = new JSONArray();
			ThreadInfo deadThreadInfo[] = threadBean.getThreadInfo(
					deadlockedThreads, true, true);
			for (ThreadInfo deadThread : deadThreadInfo) {
				dead.put(new JSONObject()
						.put("name", deadThread.getThreadName())
						.put("id", deadThread.getThreadId())
						.put("text", deadThread.toString()));
			}
			threads.put("dead", dead);
		}

		Map<Thread, StackTraceElement[]> s = Thread.getAllStackTraces();

		JSONObject threadList = new JSONObject();
		
		for (Map.Entry<Thread,StackTraceElement[]> e: s.entrySet()) {
			Thread t = e.getKey();
			JSONObject thread = new JSONObject()
					.put("state",t.getState())
					.put("name",t.getName())
					.put("stack",new JSONArray(e.getValue()));
			threadList.put(Long.toString(t.getId()),thread);
		}
		threads.put("all",threadList);
		new JSONWriter(out).object().key("threads").value(threads)
				.endObject();
	} else {
		response.sendError(500, "Unknown action.");
	}
%>