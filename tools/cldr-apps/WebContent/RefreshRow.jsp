<%@page import="org.unicode.cldr.web.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*"%>
<!-- Copyright (C) 2012 IBM and Others. All Rights Reserved -->
<!--  RefreshRow.jsp -->
<%
    WebContext ctx = new WebContext(request, response);
    String what = request.getParameter(SurveyAjax.REQ_WHAT);
    String sess = request.getParameter(SurveyMain.QUERY_SESSION);
    String loc = request.getParameter(SurveyMain.QUERY_LOCALE);
    String xpath = request.getParameter(SurveyForum.F_XPATH);
    String voteinfo = request.getParameter("voteinfo");
    String vhash = request.getParameter("vhash");
    String fieldHash = request.getParameter(SurveyMain.QUERY_FIELDHASH);
    CookieSession mySession = null;
    mySession = CookieSession.retrieve(sess);
    if (mySession == null) {
        response.sendError(500, "Session missing");
        return;
    }
    int id = Integer.parseInt(xpath);
    String xp = mySession.sm.xpt.getById(id);
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
 DataSection section = ctx.getSection(XPathTable.xpathToBaseXpath(xp),Level.COMPREHENSIVE.toString(),WebContext.LoadingShow.dontShowLoading);
/*         DataSection section = DataSection.make(null, mySession, locale,
                xp, false, Level.COMPREHENSIVE.toString());
 */        // r.put("testResults", JSONWriter.wrap(result));
        //r.put("testsRun", cc.toString());
        int oldSize  = section.getAll().size();
        DataSection.DataRow row = section.getDataRow(xp);
        if (row != null) {
            if(voteinfo!=null&&voteinfo.length()>0) {
                row.showVotingResults(ctx);
            } else {
	            row.showDataRow(ctx, ctx.getUserFile(), true, null, zoomedIn,
	                    DataSection.kAjaxRows);
            }
            ctx.flush();

            if(false){
            %>
            <td>
            ROw: <%= row %><br>
            current: <%= row.getCurrentItem() %>
            uf: <%= ctx.getUserFile().cldrfile.isEmpty() %>
            section size: <%= section.getAll().size() %> (was <%= oldSize %>), 
            xpath: <%= section.xpathPrefix %>
            skippedDueToCoverage: <%= section.skippedDueToCoverage %>,
            items: <%= row.items.size() %>
            </td>

            <% }
        } else {
            response.sendError(500, "Row not found");
        }
    }



%>