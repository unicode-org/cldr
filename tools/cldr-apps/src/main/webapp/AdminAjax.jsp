<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%><%@page
	import="org.unicode.cldr.web.*,java.util.Map,java.util.Set,java.util.Comparator,java.util.TreeMap,java.util.TreeSet"%><%@ page
	language="java" contentType="application/json; charset=UTF-8"
	import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.json.*"%><%--  Copyright (C) 2012 IBM and Others. All Rights Reserved 
	 --%><%@ page import="java.lang.management.*"%><%
	 CLDRConfigImpl.setUrls(request);
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
            sess.put("lastAction", SurveyMain.timeDiff(cs.getLastAction()));
            sess.put("timeTillKick", cs.timeTillKick());
			//			sess.put("locales",new JSONArray().put(cs.getLocales().keys()));
			users.put(cs.id, sess);
		}
		new JSONWriter(out).object().key("users").value(users)
				.endObject();
// 	} else if (action.equals("verifycheckout")) {
// 		CLDRConfig cconfig = CLDRConfig.getInstance();
		
// 		CookieSession.sm.ensureOrCheckout(out, "CLDR_DIR", new java.io.File(cconfig.getProperty("CLDR_DIR")), SurveyMain.CLDR_DIR_REPOS);
	} else if (action.equals("unlink")) {
		String s= request.getParameter("s");
		CookieSession cs = CookieSession.retrieveWithoutTouch(s);
		if(cs != null) {
			JSONObject sess = new JSONObject();
			if (cs.user != null) {
				sess.put("user", SurveyAjax.JSONWriter.wrap(cs.user));
			}
			sess.put("id", cs.id);
			sess.put("ip", cs.ip);
            sess.put("last", SurveyMain.timeDiff(cs.last));
            sess.put("lastAction", SurveyMain.timeDiff(cs.getLastAction()));
            sess.put("timeTillKick", cs.timeTillKick());

            new JSONWriter(out).object().key("kick").value(s).key("removing").value(sess)
			.endObject();
			cs.remove();
		} else {
			new JSONWriter(out).object().key("kick").value(s).key("removing").value(null).endObject();
		}
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
	} else if(action.equals("exceptions")) {
		JSONObject exceptions = new JSONObject();
		ChunkyReader cr = SurveyLog.getChunkyReader();
		exceptions.put("lastTime",cr.getLastTime());
		ChunkyReader.Entry e = null;
		if(request.getParameter("before")!=null) {
			Long before = Long.parseLong(request.getParameter("before"));
			e = cr.getEntryBelow(before);
		} else {
			e = cr.getLastEntry();
		}
		if(e!=null) {
			exceptions.put("entry",e);
		}
			
		new JSONWriter(out).object().key("exceptions").value(exceptions)
		.endObject();
    } else if(action.equals("settings")) {
    	CLDRConfigImpl cci = (CLDRConfigImpl)(CLDRConfig.getInstance());
        new JSONWriter(out).object().key("settings").value(new JSONObject().put("all", cci.toJSONObject())).endObject();
    } else if(action.equals("settings_set")) {
        JSONObject settings = new JSONObject();
        
        try {
        String setting=request.getParameter("setting");
        StringBuilder sb = new StringBuilder();
        java.io.Reader r = request.getReader();
        int ch;
        while((ch = r.read())>-1) {
//                       System.err.println("[post read] >> " + Integer.toHexString(ch));
             sb.append((char)ch);
        }
   //     System.err.println(request.getMethod() + " len " + request.getContentLength() + "type"+ request.getContentType() + "[ chars="+sb+"]");
          CLDRConfig cci = (CLDRConfig.getInstance());
          cci.setProperty(setting,sb.toString());
          settings.put("ok", true);
          settings.put(setting, cci.getProperty(setting));
        } catch(Throwable t) {
            SurveyLog.logException(t,"Tring to set setting ");
            settings.put("err",t.toString());
        }
        
        
        new JSONWriter(out).object().key("settings_set").value(settings)
        .endObject();
	} else {
		response.sendError(500, "Unknown action.");
	}
%>