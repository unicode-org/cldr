<%@page import="org.unicode.cldr.web.WebContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%--
If not booted, attempt to boot..
 --%>
<%
final String survURL = request.getContextPath() + "/survey";
SurveyMain sm = SurveyMain.getInstance(request);
if(SurveyMain.isBusted!=null) {
    %><a href="<%= request.getContextPath() + "/survey" %>">Survey Tool</a> is offline.<%
    return;
} else if(sm==null || !SurveyMain.isSetup) {
        %>
        <div id='st_err'></div>
        <%@include file="/WEB-INF/tmpl/ajax_status.jsp" %>
        
        Attempting to start SurveyTool..
        
        <%
            String url = request.getContextPath() + request.getServletPath(); // TODO add query
            // JavaScript based redirect
            %>
            <head>
                <title>SurveyTool: Redirect</title>
                  <script type="application/javascript">
                  window.setTimeout(function(){
                	  window.location.reload(true);                
                	    //document.location='<%= url %>' + document.location.search +  document.location.hash;
                  },10000);
                  </script>
            </head>
            <body>
            <noscript>
            <h1>
                JavaScript is required.
            </h1></noscript>
              If you are not redirected in a few seconds, you can click: <a id='redir' href='<%= url %>'>here</a> to retry, or <a id='redir2' href='<%= survURL %>'>here</a>.
              <script type="application/javascript">
                            document.getElementById("redir").href = '<%= url %>' + document.location.search +  document.location.hash;

                               dojo.ready(function(){
                                   dojo.xhrGet({url: '<%= survURL %>', load: function() {   window.location.reload(true); if(false)   window.setTimeout(function(){     window.location.search='?'+window.location.search.substr(1)+'&'; },5000);  }  }); 
                               });

                            
                            </script>
            <%
            
            if(sm!=null) {
        %>
                <%= sm.startupThread.htmlStatus() %>
                <hr>
                                
        <%
            }
        return;
}else
%>
<%--
    Validate the session. If we don't have a session, go back to SurveyMain.
 --%>
 <%
 WebContext ctx = new WebContext(request,response);
 String status = ctx.setSession();
if(false) { // if we need to redirect for some reason..
	 ctx.addAllParametersAsQuery();
 	 String url = request.getContextPath() + "/survey?" + ctx.query().replaceAll("&amp;", "\\&") + "&fromv=true";
	 // JavaScript based redirect
	 %>
	 <head>
    	 <title>SurveyTool: Redirect</title>
	       <script type="application/javascript">
	        document.location='<%= url %>' + document.location.hash;
	       </script>
     </head>
	 <body>
	   If you are not redirected, please click: <a href='<%= url %>'>here</a>, or <a id='redir2' href='<%= survURL %>'>here</a> (may lose your place).
	 <%
 }
%>
<html class='claro'>
<head class='claro'>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CLDR  <%= ctx.sm.getNewVersion() %> SurveyTool | view</title>

<meta name='robots' content='noindex,nofollow'>
<meta name="gigabot" content="noindex">
<meta name="gigabot" content="noarchive">
<meta name="gigabot" content="nofollow">
<link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />
<%@include file="/WEB-INF/tmpl/ajax_status.jsp" %>
<script type="text/javascript">
// set from incoming session
surveySessionId = '<%= ctx.session.id %>';
survURL = '<%= survURL %>';
  showV();
</script>
</head>
<body class='claro'>
 
        <% if( ctx.session == null || ctx.session.user == null) { %>
        <form id="login" method="POST" action="<%= request.getContextPath() + "/survey" %>">
           <%@ include file="/WEB-INF/tmpl/small_login.jsp"    %>
            
           </form>
          <% } %>
 <div data-dojo-type="dijit/layout/BorderContainer" data-dojo-props="design:'sidebar', gutters:true, liveSplitters:true" id="borderContainer">
    <div id="topstuff" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'top'" >
    <% if(status!=null) { %>
        <div class="v-status"><%= status %></div>
        <% } %>

        <%-- abbreviated form of usermenu  --%>
        <div id='toptitle'>
                <span class='title-cldr'>CLDR <%= ctx.sm.getNewVersion() %> Survey Tool
        <%=  (ctx.sm.phase()!=SurveyMain.Phase.SUBMIT)?ctx.sm.phase().toString():"" %>
         </span>

        <span class='titlePart'>
            <a class='notselected' href='<%= survURL  %>'>Locales</a>
        </span>
      
         <div id='title-locale' data-dojo-type="dijit/form/DropDownButton">
              <span>(locale)</span>
              <div id='menu-locale' data-dojo-type="dijit/DropDownMenu">
                        <div data-dojo-type="dijit/MenuItem"
                            data-dojo-props=" onClick:function(){  window.location='<%= survURL %>?_=' + surveyCurrentLocale;    }">
                            General Info</div>
              </div>
         </div>
         
         <div id='title-section' data-dojo-type="dijit/form/DropDownButton">
              <span>(section)</span>
              <div id='menu-section' data-dojo-type="dijit/DropDownMenu"></div>
         </div>

         <div id='title-page' data-dojo-type="dijit/form/DropDownButton">
              <span>(page)</span>
              <div id='menu-page' data-dojo-type="dijit/DropDownMenu"></div>
         </div>
         
         <span class='titlePart'  id='title-item'></span>
         
        </div> <%-- end of toptitle --%>         
        <div id="lowerstuff">
            <% if(ctx.session !=null && ctx.session.user != null) {
                  boolean haveCookies = (ctx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&ctx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
                  String cookieMessage = haveCookies?"<!-- and Forget Me-->":"";
              %>
              <span  class='userinfo'>
               <span class='user_email'>&lt;<%= ctx.session.user.email %>&gt;</span>
               <span class='user_name'><%= ctx.session.user.name %></span>
               <span class='user_org'>(<%= ctx.session.user.org %>)</span>
              </span>
              
               <a class='notselected' href='<%= ctx.base() + "?do=logout" %>'>Logout<%= cookieMessage %></a>
               |
            <% } %>
            <a class='notselected' href='<%= survURL  %>?do=options'>Manage</a> 
            |
            <a id='generalHelpLink' class='notselected'  href='<%= SurveyMain.GENERAL_HELP_URL %>'><%= SurveyMain.GENERAL_HELP_NAME %></a>
					|

					<div data-dojo-type="dijit/form/DropDownButton" id="reportMenu">
					<span>Review</span>
					<div data-dojo-type="dijit/DropDownMenu">
					         <% for (SurveyMain.ReportMenu m : SurveyMain.ReportMenu.values()) { %>
						<div data-dojo-type="dijit/MenuItem"
							data-dojo-props=" onClick:function(){
                                                window.location='<%= survURL + "?" + m.urlQuery() %>&_=' + surveyCurrentLocale;
                                        }"><%= m.display() %></div>
          <% } %>
                    </div>
                   </div>

            <label id='title-coverage'>
                Coverage:
            </label>
        </div> <%-- end of lowerstuff --%>
    </div> <%-- end of topstuff --%>
    <div id="DynamicDataSection" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'center'" ></div>
    <div id="itemInfo" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'trailing'" ></div>
    <div id="botstuff" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'bottom'" >
         <%@include file="/WEB-INF/tmpl/stnotices.jspf" %>
    </div>
</div>
</body>
</html>