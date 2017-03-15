<%@ page contentType="text/html; charset=UTF-8"
	import="org.unicode.cldr.web.*,org.unicode.cldr.util.*,java.io.File"%>
<%
String vap = request.getParameter("vap");
if(vap==null ||
	(SurveyMain.vap==null||SurveyMain.vap.isEmpty()) ||
    vap.length()==0 ||
			(!SurveyMain.vap.equals(vap) )  ) {
	response.sendRedirect("http://cldr.unicode.org"); // Get out.
	return;
}

String action  = request.getParameter("do");

String lmi = request.getContextPath()+"/survey?letmein="+vap+"&amp;email=admin@";
String sql = request.getContextPath()+"/survey?sql="+vap+"";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Survey Tool Administration | <%= SurveyMain.localhost() %></title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
</head>
<body class='admin'>
				<a id='gotoSt' href='<%= lmi %>'><img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" /></a>
<h1>Survey Tool Administration | <%= SurveyMain.localhost() %></h1>
<a href='<%= lmi %>'>SurveyTool as Admin</a> | <a href='<%= sql %>'>Raw SQL</a>
<%
	if(SurveyMain.testpw!=null&&!SurveyMain.testpw.isEmpty()) {
		%>
			| <a href='createAndLogin.jsp?vap=<%= SurveyMain.testpw %>'>CreateAndLogin (CLDR_TESTPW)</a>
		<%
	} else {
		%>
		| <a href='createAndLogin.jsp?vap=<%= vap %>'>CreateAndLogin (Admin Password!)</a> (set CLDR_TESTPW if you prefer having a test password)
	<%
	}
%>
		| <a href='cldr-setup.jsp?vap=<%= vap %>'>SurveyTool Setup</a>

<hr>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<div style='float: right; font-size: x-small;'>
<span id="visitors"></span>
</div>
<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>

<hr>

<% if(action!=null&&!action.isEmpty()) { %>
<a href="<%= request.getContextPath() + request.getServletPath()  + "?vap="+vap+"#!admin_ops" %>">Return to Admin Panel</a> | <h3><%= action %></h3>


<% if(action.equals("rawload")) {
%>
	<% if(CLDRConfig.getInstance().getEnvironment()!=CLDRConfig.Environment.SMOKETEST) { %>
	    <h1>Only available in SMOKETEST context. </h1>
	 <% } else { %>
	 
	 
	       <%
           String users = request.getParameter("users");
           String pxml = request.getParameter("pxml");
	       if(users!=null&&pxml!=null&&!users.isEmpty()&&!pxml.isEmpty()) {
	    	    %>
	    	          <h3>Starting import from <%= users %></h3>
                      <%
                          int usersRead = CookieSession.sm.reg.readUserFile(CookieSession.sm, new java.io.File(users));
                      %>
                <%= usersRead %>  user files read.      
                      <h3>Starting import from <%= pxml %></h3>
                      <%
                      File pxmlF = new File(pxml);
                      File common = new File(pxml, "common/main");
                      File seed = new File(pxml, "seed/main"); 
                      File dirs[] = { common,seed };
                      Integer ret[] = CookieSession.sm.getSTFactory().readPXMLFiles(dirs);
                      int locsUpdateC = ret[0];
                      int locsUpdateS = ret[1];
                      %>
                <%= locsUpdateC %>  locales loaded from <%= pxml %>/common/main and       
                <%= locsUpdateS %>  locales loaded from <%= pxml %>/seed/main
	    	          
	    	    <%	    	   
	    	    
	    	    if(true) {
	    	    	%>
	    	    	   <hr>
	    	    	   <b>NOTE: This process purposefully causes the SurveyTool to enter a "broken" state, so that it isn't used in an inconsistent manner. The message "WARNING: SurveyTool busted: Due to IMPORT of local data" is expected and due to successful import.
	    	    	   </b>
	    	    	   <br>
	    	    	    ... Now busting (shutting down) your surveytool so that this will be picked up the next time around
	    	    	    
	    	    	    <div class='warnText'>You must manually restart the SurveyTool to proceed. (i.e. restart Tomcat)</div>'
	    	    	<%
	    	    	  CookieSession.sm.busted("Due to IMPORT of local data from " + users + " and " + pxml);
	    	    }
	       }
	       %>
	 
	 <hr>
    <form method='POST' action='<%= request.getContextPath() + request.getServletPath() %>'>
        <i>REPLACE ALL LOCAL DATA with users and votes as follows.  Does NOT update SVN data.</i><br>
        
                <label>ABSOLUTE PATH to users.xml or usersa.xml file: <input name='users' size=80></label>
                        <i>See: <a href='http://www.unicode.org/repos/cldr-tmp2/usersa/usersa.xml'>http://www.unicode.org/repos/cldr-tmp2/usersa/usersa.xml</a></i>
                    <br>
                <label>ABSOLUTE Path to PXML directory: <input name='pxml' size=80></label>
                    <i>See: <a href='http://www.unicode.org/repos/cldr-tmp2/pxml/'>http://www.unicode.org/repos/cldr-tmp2/pxml/</a></i>
                    <br>
                
        <input type='hidden' value='<%= vap %>' name='vap'/>
        <input type='hidden' value='<%= action %>' name='do'/>
        <input type='submit'>
        <i>Clicking submit will take a couple minutes.</i>
    </form>

  <% }  %>
<% } else { %>
    <h4>Unknown action: <%= action %></h4>
<% } %>

<hr>
<a href="<%= request.getContextPath() + request.getServletPath()  + "?vap="+vap+"#!admin_ops" %>">Return to Admin Panel</a>
<% } else { %>
<div class='fnotebox'>
    For instructions, see <a href='http://cldr.unicode.org/index/survey-tool/admin'>Admin Docs</a>. <br>
    Tabs do not (currently) auto update. Click a tab again to update. <br>
    Be careful!
</div>
<script>
var vap='<%= vap %>';
dojo.ready(loadAdminPanel);
</script>

<div id='adminStuff'></div>

<% } %>

</body>
</html>