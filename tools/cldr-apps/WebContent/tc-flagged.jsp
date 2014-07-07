<%@ page language="java" contentType="text/html; charset=UTF-8"
	import='java.sql.Connection,java.util.Map,org.unicode.cldr.util.*'
    pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/session.jspf" %>
<% if(cs.user.userlevel > UserRegistry.TC) { response.sendRedirect(surveyUrl); return; } %>
<html>
	<head>
		<title>Unicode | Flagged Items</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	<%@include file="/WEB-INF/tmpl/ajax_status.jsp" %>
	</head>
    
    <body>
<%

Connection conn = null;
	try {
		int n =0;
		conn = cs.sm.dbUtils.getDBConnection();

		Map<String,Object> l[] = cs.sm.dbUtils.sqlQueryArrayAssoc(conn,"select * from "+ DBUtils.Table.VOTE_FLAGGED +"  order by last_mod desc");
%>
<a href="<%=request.getContextPath()%>/survey?do=list">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>

<div class='flaggedItemsBox'>
<h2><img src='flag.png' /><%= l.length %> flagged items in This  <%=  cs.sm.getNewVersion() %> Release</h2>


<table class='data flaggedItems'>
<%		
		for(Map<String,Object> m : l) {
			CLDRLocale locale = CLDRLocale.getInstance(m.get("locale").toString());
			int xpathId = (Integer)m.get("xpath");
			String xpath = cs.sm.xpt.getById(xpathId);
			PathHeader ph = cs.sm.getSTFactory().getPathHeader(xpath);
			int submitter = (Integer)m.get("submitter");
			UserRegistry.User user = cs.sm.reg.getInfo(submitter);
	        StringBuilder linkurl = new StringBuilder(request.getContextPath());
	        String strid = cs.sm.xpt.getStringIDString(xpathId);
	        WebContext.appendContextVurl(linkurl, locale, ph.getPageId(), strid, "");
			%>
			<tr class='r<%= ((n++) % 2) %>'>
				<td class='flaggedZoom'>
				</td>
				<th class='flaggedLocale'><%= locale.getDisplayName() %></th>
				<th class='flaggedPath'><a href='<%= linkurl %>'><%= ph %></a><br><tt class='codebox' title='<%= xpathId %>'><%= xpath %></tt>
						</th>
				<td class='flaggedId' id='<%=locale.toString()+"."+xpathId%>'>
					When: <%= m.get("last_mod") %>
			           <script>
			            dojo.byId('<%=locale.toString()+"."+xpathId%>').appendChild(createUser( <%= user.toJSONString() %> ));
			           </script>
				</td>
			</tr>
			<%
		}
		
	} finally {
		DBUtils.close(conn);
	}

%>
</table>

</div>

</body>
</html>
