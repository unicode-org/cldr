<%@page import="org.unicode.cldr.web.*"%><%@ page language="java" contentType="text/html; charset=UTF-8"
	import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.json.*"%><%--  Copyright (C) 2012 IBM and Others. All Rights Reserved  --%><%WebContext ctx = new WebContext(request, response);
			String what = request.getParameter(SurveyAjax.REQ_WHAT);
			String sess = request.getParameter(SurveyMain.QUERY_SESSION);
			String loc = request.getParameter(SurveyMain.QUERY_LOCALE);
			String xpath = WebContext.decodeFieldString(request.getParameter(SurveyForum.F_XPATH));
			String voteinfo = request.getParameter("voteinfo");
			String vhash = request.getParameter("vhash");
			String fieldHash = request.getParameter(SurveyMain.QUERY_FIELDHASH);
			String covlev = request.getParameter("p_covlev");
			Level coverage = Level.COMPREHENSIVE;
			if(covlev!=null && covlev.length()>0) {
				coverage = Level.get(covlev);
			}
			CookieSession mySession = null;
			mySession = CookieSession.retrieve(sess);
			boolean isJson = request.getParameter("json")!=null;
			if (isJson) {
				request.setCharacterEncoding("UTF-8");
				response.setCharacterEncoding("UTF-8");
				response.setContentType("application/json");
			}

			if (mySession == null) {
				if(!isJson) {
					response.sendError(500, "Your session has timed out or the SurveyTool has restarted.");
				} else {
					JSONWriter r = new JSONWriter(out).object().
							key("err").value("Your session has timed out or the SurveyTool has restarted.").endObject();
				}
				return;
			}
			String xp = xpath;
			try {
				int id = Integer.parseInt(xpath);
				xp = mySession.sm.xpt.getById(id);
			} catch (NumberFormatException nfe) {

			}
			ctx.session = mySession;
			ctx.sm = ctx.session.sm;
			ctx.setServletPath(ctx.sm.defaultServletPath);
			CLDRLocale locale = CLDRLocale.getInstance(loc);
			ctx.setLocale(locale);

			boolean dataEmpty = false;
			boolean zoomedIn = request.getParameter("zoomedIn") != null
					&& request.getParameter("zoomedIn").length() > 0;
			synchronized (mySession) {
				/*         SurveyMain.UserLocaleStuff uf = mySession.sm.getUserFile(
				 mySession, locale);
				 */
				DataSection section = null;
				try {
					section = ctx.getSection(XPathTable.xpathToBaseXpath(xp),
							coverage.toString(),
							WebContext.LoadingShow.dontShowLoading);
				} catch (Throwable t) {
					SurveyLog.logException(t);
					if(!isJson) {
						response.sendError(500, "Exception on getSection:"+t.toString());
					} else {
						JSONWriter r = new JSONWriter(out).object().
								key("err").value("Exception on getSection:"+t.toString()).endObject();
					}
					return;
				}

				if (request.getParameter("json") != null) {
					request.setCharacterEncoding("UTF-8");
					response.setCharacterEncoding("UTF-8");
					response.setContentType("application/json");
					JSONObject dsets = new JSONObject();
					{
						XPathMatcher matcher = XPathMatcher.getMatcherForString(xp);
						for (String n : SortMode.getSortModesFor(xp)) {
							dsets.put(
									n,
									section.createDisplaySet(
											SortMode.getInstance(n), matcher));
						}
						//DataSection.DisplaySet ds = section.getDisplaySet(ctx, matcher);
						dsets.put("default", SortMode.getSortMode(ctx, section));
					}
					JSONWriter r = new JSONWriter(out).object().key("section")
							.value(section).key("displaySets").value(dsets)
							.key("dir").value(ctx.getDirectionForLocale())
							.key("canModify").value(ctx.canModify())
							.endObject();
					return;
				}

				/*         DataSection section = DataSection.make(null, mySession, locale,
				 xp, false, Level.COMPREHENSIVE.toString());
				 */// r.put("testResults", JSONWriter.wrap(result));
					//r.put("testsRun", cc.toString());
				int oldSize = section.getAll().size();
				DataSection.DataRow row = section.getDataRow(xp);
				if (row != null) {
					if (voteinfo != null && voteinfo.length() > 0) {
						row.showVotingResults(ctx);
					} else {
						row.showDataRow(ctx, ctx.getUserFile(), true, null,
								zoomedIn, DataSection.kAjaxRows);
					}
					ctx.flush();

					if (false) {%><td>
            ROw: <%=row%><br>
            current: <%=row.getCurrentItem()%>
            uf: <%=ctx.getUserFile().cldrfile.isEmpty()%>
            section size: <%=section.getAll().size()%> (was <%=oldSize%>), 
            xpath: <%=section.xpathPrefix%>
            skippedDueToCoverage: <%=section.skippedDueToCoverage%>,
            items: <%=row.items.size()%>
            </td>

            <%
            	}
            		} else {
    					if(!isJson) {
    						response.sendError(500, "Row not found");
    					} else {
    						JSONWriter r = new JSONWriter(out).object().
    								key("err").value("Row not found.").endObject();
    					}
    					return;
            		}
            	}
            %>